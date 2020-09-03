package services.zookeeper

import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import com.evidence.service.common.zookeeper.ServiceEndpoint
import com.google.inject.{Inject, Provider}
import com.typesafe.config.Config
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.PathChildrenCache
import org.joda.time.{DateTime, DateTimeZone}

import scala.concurrent.Future

class HeimdallLoadBalancer(nodeAndPerftrackAware: Map[Int, (PathChildrenCacheFacade, PathChildrenCacheFacade)],
                           config: HeimdallLoadBalancerConfig) extends StrictStatsD with LazyLogging {

  private[this] final val reloadIntervalMs = config.reloadIntervalMs
  private[this] var rtmCache = Map[Int, ZookeeperNodeCache[ServiceEndpoint]]()
  private[this] var perftrakCache = Map[Int, ZookeeperNodeCache[PerftrakDatum]]()
  private[this] var endpointResolver = Map[Int, AtomicReference[EndpointResolver]]()
  private[this] var lastReloadTimestamp = Map[Int, AtomicLong]()


  def start(): Unit = {
    for ((k, v) <- nodeAndPerftrackAware) {
      rtmCache = rtmCache + (k -> new RtmZookeeperNodeCache(v._1, () => HeimdallLoadBalancer.this.reload(k)))
      perftrakCache = perftrakCache + (k -> new PerftrakZookeeperNodeCache(v._2, () =>  HeimdallLoadBalancer.this.reload(k)))
      endpointResolver = endpointResolver + (k -> new AtomicReference(new EndpointResolver(config.enableCache)))
      lastReloadTimestamp = lastReloadTimestamp + (k -> new AtomicLong(0))

      rtmCache(k).start()
      perftrakCache(k).start()
      reload(k)
    }
  }

  def reload(version: Int): Unit = {
    val nowTimestamp: Long = DateTime.now(DateTimeZone.UTC).getMillis
    val lastTimestamp: Long = lastReloadTimestamp(version).get()
    val timeElapsed: Long = nowTimestamp - lastTimestamp
    if (timeElapsed > reloadIntervalMs && lastReloadTimestamp(version).compareAndSet(lastTimestamp, nowTimestamp)) {
      statsd.increment("load_balancer_data_reload")
      endpointResolver(version).set(EndpointResolver(rtmCache(version).getData, perftrakCache(version).getData, config.enableCache))
    }
  }

  def stop(): Unit = {
    nodeAndPerftrackAware.keySet.foreach(rtmVersion => {
      rtmCache(rtmVersion).stop()
      perftrakCache(rtmVersion).stop()
    })
  }

  def getInstanceAsFuture(key: String, version: Int): Future[ServiceEndpoint] = {
    getInstance(key, version) match {
      case Some(server) => Future.successful(server)
      case _ => Future.failed(new Exception(s"Failed to get an instance for key=$key, apiVersion=$version"))
    }
  }

  def getInstance(key: String, version: Int): Option[ServiceEndpoint] =
    endpointResolver(version).get.get(key.replace("-", "").toLowerCase)

  /**
    * Exposing replica counts for unit testing.
    */
  def getReplicaCounts(version: Int): Map[ServiceEndpoint, Int] =
    endpointResolver(version).get.getReplicaCounts

}

object HeimdallLoadBalancer extends LazyLogging {

  def apply(config: Config, client: CuratorFramework): HeimdallLoadBalancer = {
    val heimdallConfig    = HeimdallLoadBalancerConfig(config)
    var nodeAndPerftrackAware = Map[Int, (PathChildrenCacheFacade, PathChildrenCacheFacade)]()

    var rtmVersion = 1
    val rtm = new PathChildrenCache(client, "/service/rtm/http", true)
    val perftrack = new PathChildrenCache(client, "/service/rtm/http/perftrak", true)
    nodeAndPerftrackAware += rtmVersion -> (new PathChildrenCacheAdapter(rtm), new PathChildrenCacheAdapter(perftrack))

    // do one more if enableRTMv2
    if (heimdallConfig.enableRTMv2) {
      rtmVersion = 2
      val rtm = new PathChildrenCache(client, s"/service/rtmv2/http", true)
      val perftrack = new PathChildrenCache(client, s"/service/rtmv2/http/perftrak", true)
      nodeAndPerftrackAware += rtmVersion -> (new PathChildrenCacheAdapter(rtm), new PathChildrenCacheAdapter(perftrack))
    }

    logger.info("creatingHeimdallLoadBalancer")("heimdallConfig" -> heimdallConfig)

    val loadBalancer = new HeimdallLoadBalancer(
      nodeAndPerftrackAware,
      heimdallConfig)
    loadBalancer.start()
    loadBalancer
  }

}

class HeimdallLoadBalancerProvider @Inject()(config: Config, client: CuratorFramework)
    extends Provider[HeimdallLoadBalancer] {
  def get(): HeimdallLoadBalancer = HeimdallLoadBalancer(config, client)
}
