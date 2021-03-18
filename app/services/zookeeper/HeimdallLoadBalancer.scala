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
import scala.util.Try

class HeimdallLoadBalancer(
  nodeAndPerftrackAware: Map[Int, (PathChildrenCacheFacade, PathChildrenCacheFacade)],
  config: HeimdallLoadBalancerConfig)
    extends StrictStatsD
    with LazyLogging {

  private[this] final val reloadIntervalMs = config.reloadIntervalMs
  private[this] var rtmCache               = Map[Int, ZookeeperNodeCache[ServiceEndpoint]]()
  private[this] var perftrakCache          = Map[Int, ZookeeperNodeCache[PerftrakDatum]]()
  private[this] var endpointResolver       = Map[Int, AtomicReference[EndpointResolver]]()
  private[this] var lastReloadTimestamp    = Map[Int, AtomicLong]()

  def start(): Unit = {
    for ((k, v) <- nodeAndPerftrackAware) {
      rtmCache = rtmCache + (k -> new RtmZookeeperNodeCache(v._1, () => HeimdallLoadBalancer.this.reload(k)))
      perftrakCache = perftrakCache + (k -> new PerftrakZookeeperNodeCache(
        v._2,
        () => HeimdallLoadBalancer.this.reload(k)))
      endpointResolver = endpointResolver + (k       -> new AtomicReference(EndpointResolver()))
      lastReloadTimestamp = lastReloadTimestamp + (k -> new AtomicLong(0))

      rtmCache(k).start()
      perftrakCache(k).start()
      reload(k)
    }
  }

  def reload(version: Int): Unit = {
    val nowTimestamp: Long  = DateTime.now(DateTimeZone.UTC).getMillis
    val lastTimestamp: Long = lastReloadTimestamp(version).get()
    val timeElapsed: Long   = nowTimestamp - lastTimestamp
    if (timeElapsed > reloadIntervalMs && lastReloadTimestamp(version).compareAndSet(lastTimestamp, nowTimestamp)) {
      statsd.increment("load_balancer_data_reload")
      endpointResolver(version).set(
        EndpointResolver(rtmCache(version).getData, perftrakCache(version).getData, config.enableCache))
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
      case _            => Future.failed(new Exception(s"Failed to get an instance for key=$key, apiVersion=$version"))
    }
  }

  def getInstance(key: String, version: Int): Option[ServiceEndpoint] = {
    Try(endpointResolver(version).get).toOption match {
      case Some(endpointResolver) => endpointResolver.get(key.replace("-", "").toLowerCase)
      case _                      => None
    }
  }

  /**
    * Exposing replica counts for unit testing.
    */
  def getReplicaCounts(version: Int): Map[ServiceEndpoint, Int] =
    Try(endpointResolver(version).get).toOption match {
      case Some(endpointResolver) => endpointResolver.getReplicaCounts
      case _                      => Map()
    }

}

object HeimdallLoadBalancer extends LazyLogging {

  def apply(config: Config, client: CuratorFramework): HeimdallLoadBalancer = {
    val heimdallConfig        = HeimdallLoadBalancerConfig(config)
    var nodeAndPerftrackAware = Map[Int, (PathChildrenCacheFacade, PathChildrenCacheFacade)]()

    // we will enable Heimdall to handle both v1 and v2 of RTM
    val rtmV1       = new PathChildrenCache(client, "/service/rtm/http", true)
    val perftrackV1 = new PathChildrenCache(client, "/service/rtm/http/perftrak", true)
    nodeAndPerftrackAware += 1 -> (new PathChildrenCacheAdapter(rtmV1), new PathChildrenCacheAdapter(perftrackV1))

    val rtmV2       = new PathChildrenCache(client, s"/service/rtmv2/http", true)
    val perftrackV2 = new PathChildrenCache(client, s"/service/rtmv2/http/perftrak", true)
    nodeAndPerftrackAware += 2 -> (new PathChildrenCacheAdapter(rtmV2), new PathChildrenCacheAdapter(perftrackV2))

    logger.info("creatingHeimdallLoadBalancer")("heimdallConfig" -> heimdallConfig)

    val loadBalancer = new HeimdallLoadBalancer(nodeAndPerftrackAware, heimdallConfig)
    loadBalancer.start()
    loadBalancer
  }

}

class HeimdallLoadBalancerProvider @Inject()(config: Config, client: CuratorFramework)
    extends Provider[HeimdallLoadBalancer] {
  def get(): HeimdallLoadBalancer = HeimdallLoadBalancer(config, client)
}
