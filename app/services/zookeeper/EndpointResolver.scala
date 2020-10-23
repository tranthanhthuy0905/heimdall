package services.zookeeper

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import com.evidence.service.common.zookeeper.ServiceEndpoint
import org.joda.time.{DateTime, DateTimeZone}

import scala.util.{Success, Try}

/**
  * EndpointResolver receives RTM and perftrak data to calculate map of RTM endpoints to number of hash replicas.
  *
  * The replica number is based on a node's priority value.
  *
  * Priority (p) is calculated as current component aggregate (a)
  * value divided by node capacity (c) multiplied by (-1):
  * p = -1 * a / c.
  *
  * Aggregate value shows how busy current node is.
  * I.e. greater the value, busier the node.
  * Capacity is constant for a node. Moreover, with current terraform setup it is constant for
  * all nodes of an environment.
  *
  * RTM calculates capacity as a number of logical CPUs * max number of streams per core.
  * For instance, capacity value equals to 128 for every RTM node in US1.
  *
  * It is sufficient to use aggregate value only for calculating number of hash replicas.
  * An advantage of using capacity is to allow heterogeneous RTM setup.
  *
  * (-1) is used to allow priority to be in direct relationship with the number of replicas.
  * This is also optional and can be removed.
  * Note: if (-1) is removed, the priority value to number of replicas relationship will be inverse.
  *
  * With the current way of calculating aggregate values by RTM, priority values usually end up in
  * the range of [-1, 0]. In some extreme load cases priorities < -1 are possible.
  * So, number of replicas is calculated as a priority value mapped to [1, 1001] range.
  * In effect, each node gets from 1 to 1001 hash replicas.
  *
  * Performing simple request distribution tests and measuring reshuffling showed that the hash right gives
  * best results on 500 to 1000 of replicas per node.
  * Current range is set to 1 to 1001 to allow loaded RTM nodes to receive close to 0 requests.
  * This range won't guarantee precise distribution of request proportionally to the replica counts,
  * but it'll help to disable loaded RTM nodes, while still providing good request distribution among the
  * lightly loaded nodes.
  */
class EndpointResolver(
  endpoints: List[ServiceEndpoint],
  perftrakData: List[PerftrakDatum],
  priorityMap: Map[ServiceEndpoint, Double],
  defaultPriority: Double,
  maxPriority: Double,
  enableCache: Boolean)
    extends LazyLogging
    with StrictStatsD {

  private[this] final val (minReplicaCount, maxReplicaCount) = (1.0, 1001.0)

  private[this] val rand          = new scala.util.Random(DateTime.now(DateTimeZone.UTC).getMillis)
  private[this] val replicaCounts = initReplicaCounts(endpoints, perftrakData, priorityMap, defaultPriority, maxPriority)
  private[this] val hashRing      = new ConsistentHashRing[ServiceEndpoint](endpointStringer, replicaCounts)
  private[this] val topContributorsCacheOpt =
    initTopContributorsCache(enableCache, perftrakData, priorityMap, defaultPriority)

  randomlyLogDetails()

  def this(enableCache: Boolean) {
    this(List[ServiceEndpoint](), List[PerftrakDatum](), Map[ServiceEndpoint, Double](), 0.0, 0.0, false)
  }

  def get(key: String): Option[ServiceEndpoint] = {
    val wasCached = topContributorsCacheOpt.getOrElse(Map[String, ServiceEndpoint]().empty).contains(key)
    statsd.increment("get_endpoint", s"was_cached:$wasCached")

    if (wasCached) {
      Some(topContributorsCacheOpt.get(key))
    } else {
      if (hashRing.isEmpty) {
        logger.error("noServersAvailable")("key" -> key)
        None
      } else {
        logger.debug("gettingEndpointFromHashRing")(
          "key"     -> key,
          "details" -> "Key was not found in cache, using hash ring to get endpoint")
        hashRing.get(key)
      }
    }
  }

  def getReplicaCounts: Map[ServiceEndpoint, Int] = replicaCounts

  private[this] def initTopContributorsCache(
    enableCache: Boolean,
    perftrakData: List[PerftrakDatum],
    priorityMap: Map[ServiceEndpoint, Double],
    defaultPriority: Double): Option[Map[String, ServiceEndpoint]] = {

    if (enableCache) {
      var topContributingKeysMap = scala.collection.mutable.Map[String, ServiceEndpoint]()
      perftrakData.foreach { datum =>
        if (!datum.isPlaneCachingEmpty) {
          val tops = datum.planeCaching.get.tops
          tops.foreach { top =>
            if (topContributingKeysMap.contains(top.k)) {
              if (priorityMap.getOrElse(datum.endpoint, defaultPriority) > priorityMap
                    .getOrElse(topContributingKeysMap(top.k), defaultPriority)) {
                topContributingKeysMap += (top.k -> datum.endpoint)
              }
            } else {
              topContributingKeysMap += (top.k -> datum.endpoint)
            }
          }
        }
      }
      Some(topContributingKeysMap.toMap)
    } else {
      None
    }
  }

  private[this] def initReplicaCounts(
    endpoints: List[ServiceEndpoint],
    perftrakData: List[PerftrakDatum],
    priorityMap: Map[ServiceEndpoint, Double],
    defaultPriority: Double,
    maxPriority: Double): Map[ServiceEndpoint, Int] = {
    val rangePriority = maxPriority - defaultPriority
    endpoints.foldLeft(Map[ServiceEndpoint, Int]()) { (m, endpoint) =>
      m + (endpoint -> getReplicaCount(endpoint, priorityMap, defaultPriority, rangePriority))
    }
  }

  private[this] def getReplicaCount(
    endpoint: ServiceEndpoint,
    priorityMap: Map[ServiceEndpoint, Double],
    defaultPriority: Double,
    rangePriority: Double): Int = {
    Try(priorityMap(endpoint)) match {
      case Success(value) =>
        calcReplicaCount(value, defaultPriority, rangePriority)
      case _ =>
        calcReplicaCount(defaultPriority, defaultPriority, rangePriority)
    }
  }

  private[this] def calcReplicaCount(priority: Double, defaultPriority: Double, rangePriority: Double): Int = {
    val factor = if (rangePriority > 0) {
      Math.min(1.0, Math.max(0.0, (priority - defaultPriority) / rangePriority))
    } else {
      1.0
    }
    val result = (minReplicaCount + (maxReplicaCount - minReplicaCount) * factor).round.toInt
    if (rand.nextInt(50000) == 13) {
      logger.info("replicaCount")(
        "priority" -> priority,
        "defaultPriority" -> defaultPriority,
        "rangePriority" -> rangePriority,
        "factor" -> factor
      )
    }
    result
  }

  /**
    * Repeating EcomSaas's logic of randomly printing replica counts and priority map content.
    */
  private[this] def randomlyLogDetails(): Unit = {
    if (rand.nextInt(1000) == 13) {
      logger.info("endpointResolverDetails")(
        "priorityMap"   -> priorityMap,
        "replicaCounts" -> replicaCounts
      )
    }
  }

  private[this] final def endpointStringer(e: ServiceEndpoint): String = s"https://${e.host}:${e.port}/"
}

object EndpointResolver extends LazyLogging {

  def apply(
    endpoints: List[ServiceEndpoint],
    perftrakData: List[PerftrakDatum],
    enableCache: Boolean): EndpointResolver = {

    val priorityMap = perftrakDataToPriorityMap(perftrakData)
    val (minPriority, maxPriority, sumPriority) = getMinMaxAndSum(priorityMap)
    val avgPriority = sumPriority / priorityMap.size

    if (!((avgPriority - minPriority) > -0.000001 && (avgPriority - maxPriority) < 0.000001)) {

      /** This should never happen */
      logger.warn("incorrectAggregatePriorities")(
        "minPriority" -> minPriority,
        "maxPriority" -> maxPriority,
        "avgPriority" -> avgPriority)
      new EndpointResolver(endpoints, perftrakData, priorityMap, 0.0, 0.0, enableCache)
    } else {
      /** The idea behind this calculation of thresholdPriority (instead of just taking minPriority)
        * is to make allocated number of replicas more stable.
        * From: https://git.taservs.net/ecom/ecomsaas/blob/3b916d8c56812f5524cce34e8453dfb7c3de5387/wc/com.evidence/com.evidence/transcode/Odt2BackendResolver.cs#L424
        */
      val thresholdPriority = avgPriority * Math.pow(0.7, Math.signum(avgPriority))
      new EndpointResolver(endpoints, perftrakData, priorityMap, thresholdPriority, maxPriority, enableCache)
    }
  }

  private[this] def getMinMaxAndSum(map: Map[ServiceEndpoint, Double]): (Double, Double, Double) = {
    map.foldLeft((Double.MaxValue, Double.MinValue, 0.0)) {
      case ((min, max, sum), e) => (math.min(min, e._2), math.max(max, e._2), sum + e._2.toFloat)
    }
  }

  def perftrakDataToPriorityMap(perftrakData: List[PerftrakDatum]): Map[ServiceEndpoint, Double] = {
    perftrakData.filter(!_.isPlaneComputationalEmpty).foldLeft(Map[ServiceEndpoint, Double]()) { (m, datum) =>
      m + (datum.endpoint -> (-1) * (datum.planeComputational.get.aggregate / datum.planeComputational.get.capacity))
    }
  }

}
