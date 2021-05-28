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
  rtmPathChildrenCache: PathChildrenCacheFacade,
  perftrakPathChildrenCache: PathChildrenCacheFacade,
  config: HeimdallLoadBalancerConfig)
    extends StrictStatsD
    with LazyLogging {

  private[this] final val reloadIntervalMs = config.reloadIntervalMs
  private[this] val rtmCache: ZookeeperNodeCache[ServiceEndpoint] =
    new RtmZookeeperNodeCache(rtmPathChildrenCache, () => HeimdallLoadBalancer.this.reload())
  private[this] val perftrakCache: ZookeeperNodeCache[PerftrakDatum] =
    new PerftrakZookeeperNodeCache(perftrakPathChildrenCache, () => HeimdallLoadBalancer.this.reload())
  private[this] val endpointResolver: AtomicReference[EndpointResolver] =
    new AtomicReference(EndpointResolver())
  private[this] val lastReloadTimestamp: AtomicLong = new AtomicLong(0)

  def start(): Unit = {
    rtmCache.start()
    perftrakCache.start()
    reload()
  }

  def reload(): Unit = {
    val nowTimestamp: Long  = DateTime.now(DateTimeZone.UTC).getMillis
    val lastTimestamp: Long = lastReloadTimestamp.get()
    val timeElapsed: Long   = nowTimestamp - lastTimestamp
    if (timeElapsed > reloadIntervalMs && lastReloadTimestamp.compareAndSet(lastTimestamp, nowTimestamp)) {
      statsd.increment("load_balancer_data_reload")
      endpointResolver.set(
        EndpointResolver(rtmCache.getData, perftrakCache.getData, config.enableCache))
    }
  }

  def stop(): Unit = {
    rtmCache.stop()
    perftrakCache.stop()
  }

  def getInstanceAsFuture(key: String): Future[ServiceEndpoint] = {
    getInstance(key) match {
      case Some(server) => Future.successful(server)
      case _            => Future.failed(new Exception(s"Failed to get an instance for key=$key"))
    }
  }

  def getInstance(key: String): Option[ServiceEndpoint] = {
    Try(endpointResolver.get).toOption match {
      case Some(endpointResolver) => endpointResolver.get(key.replace("-", "").toLowerCase)
      case _                      => None
    }
  }

  /**
    * Exposing replica counts for unit testing.
    */
  def getReplicaCounts(): Map[ServiceEndpoint, Int] =
    Try(endpointResolver.get).toOption match {
      case Some(endpointResolver) => endpointResolver.getReplicaCounts
      case _                      => Map()
    }

}

object HeimdallLoadBalancer extends LazyLogging {

  def apply(config: Config, client: CuratorFramework): HeimdallLoadBalancer = {
    val rtmNodeCache      = new PathChildrenCache(client, "/service/rtm/http", true)
    val perftrakNodeCache = new PathChildrenCache(client, "/service/rtm/http/perftrak", true)
    val heimdallConfig        = HeimdallLoadBalancerConfig(config)

    val loadBalancer = new HeimdallLoadBalancer(
      new PathChildrenCacheAdapter(rtmNodeCache),
      new PathChildrenCacheAdapter(perftrakNodeCache),
      heimdallConfig
    )
    loadBalancer.start()
    loadBalancer
  }

}

class HeimdallLoadBalancerProvider @Inject()(config: Config, client: CuratorFramework)
    extends Provider[HeimdallLoadBalancer] {
  def get(): HeimdallLoadBalancer = HeimdallLoadBalancer(config, client)
}
