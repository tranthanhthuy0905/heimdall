package services.zookeeper

import com.evidence.service.common.ConsistentHash
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import com.evidence.service.common.zookeeper.ServiceEndpoint
import org.joda.time.{DateTime, DateTimeZone}

import scala.math.{max, min, round}
import scala.util.{Success, Try}

/**
  * EndpointResolver receives RTM and perftrak data to calculate map of RTM endpoints to number of hash replicas.
  *
  * The replica number is based on a node's priority value.
  *
  * Priority (p) is calculated as current component aggregate (a)
  * value divided by node capacity (c) multiplied by (-1):
  * p = -1 * a / c.
  * See method perftrakDataToPriorityMap.
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
  * So, number of replicas is calculated as a priority value normalized to [1, 101] range.
  * In effect, each node gets from 1 to 101 hash replicas.
  *
  * 100 as number of replicas was chosen because it worked decently in past.
  * See EcomSaas's and service-common's implementations.
  * There is no evidence that it's optimal.
  *
  */
class EndpointResolver(
  endpoints: List[ServiceEndpoint],
  perftrakData: List[PerftrakDatum],
  priorityMap: Map[ServiceEndpoint, Double],
  defaultPriority: Double,
  enableCache: Boolean)
    extends LazyLogging
    with StrictStatsD {

  private[this] final val (minReplicaCount, maxReplicaCount) = (1.0, 101.0)

  private[this] val rand          = new scala.util.Random(DateTime.now(DateTimeZone.UTC).getMillis)
  private[this] val replicaCounts = initReplicaCounts(endpoints, perftrakData, priorityMap, defaultPriority)
  private[this] val hashRing      = new ConsistentHash[ServiceEndpoint](endpointStringer, replicaCounts)
  private[this] val topContributorsCacheOpt =
    initTopContributorsCache(enableCache, perftrakData, priorityMap, defaultPriority)

  randomlyLogDetails()

  def this(enableCache: Boolean) {
    this(List[ServiceEndpoint](), List[PerftrakDatum](), Map[ServiceEndpoint, Double](), 0.0, false)
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
    defaultPriority: Double): Map[ServiceEndpoint, Int] = {
    val (minPriority, maxPriority) = getMinMax(priorityMap)
    endpoints.foldLeft(Map[ServiceEndpoint, Int]()) { (m, endpoint) =>
      m + (endpoint -> calcNumberOfReplicas(endpoint, priorityMap, defaultPriority, minPriority, maxPriority))
    }
  }

  private[this] def calcNumberOfReplicas(
    endpoint: ServiceEndpoint,
    priorityMap: Map[ServiceEndpoint, Double],
    default: Double,
    minPriority: Double,
    maxPriority: Double): Int = {
    Try(priorityMap(endpoint)) match {
      case Success(value) =>
        normalize(value, minPriority, maxPriority)
      case _ =>
        normalize(default, minPriority, maxPriority)
    }
  }

  private[this] def getMinMax(map: Map[ServiceEndpoint, Double]): (Double, Double) = {
    map.foldLeft((0.0, 0.0)) { case ((min, max), e) => (math.min(min, e._2), math.max(max, e._2)) }
  }

  /**
    * Normalizes priority value to 1-101 range.
    */
  private[this] def normalize(priority: Double, minPriority: Double, maxPriority: Double): Int = {
    val range = maxPriority - minPriority
    if (range < 0.001) {
      /** If max almost equals to min, there is no range. Return max number of replicas. */
      maxReplicaCount.toInt
    } else {
      val value        = minReplicaCount + (maxReplicaCount - minReplicaCount) * (priority - minPriority) / range
      val boundedValue = min(max(value, minReplicaCount), maxReplicaCount)
      val result       = round(boundedValue).toInt
      logger.debug("normalizedPriority")(
        "priority"                  -> priority,
        "minPriority"               -> minPriority,
        "maxPriority"               -> maxPriority,
        "priorityRange"             -> range,
        "normalizedRawPriority"     -> value,
        "normalizedBoundedPriority" -> boundedValue,
        "result"                    -> result
      )
      result
    }
  }

  /**
    * Repeating EcomSaas's logic of randomly printing replica counts and priority map content.
    */
  private[this] def randomlyLogDetails(): Unit = {
    if (rand.nextInt(100) == 13) {
      logger.info("endpointResolverDetails")(
        "priorityMap"             -> priorityMap,
        "replicaCounts"           -> replicaCounts,
        "topContributorsCacheOpt" -> topContributorsCacheOpt,
        "perftrakData"            -> perftrakData
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

    /**
      * Choose a default value that is 70% (arbitrary amount) of the average.
      * This ensures that nodes where we aren't able to calculate a value are still eligible for traffic,
      * but are lower priority than the average of nodes where we could calculate the value.
      */
    val defaultPriority: Double = getAveragePriority(priorityMap) * 0.7
    new EndpointResolver(endpoints, perftrakData, priorityMap, defaultPriority, enableCache)
  }

  def perftrakDataToPriorityMap(perftrakData: List[PerftrakDatum]): Map[ServiceEndpoint, Double] = {
    perftrakData.filter(!_.isPlaneComputationalEmpty).foldLeft(Map[ServiceEndpoint, Double]()) { (m, datum) =>
      m + (datum.endpoint -> (-1) * (datum.planeComputational.get.aggregate / datum.planeComputational.get.capacity))
    }
  }

  def getAveragePriority(map: Map[ServiceEndpoint, Double]): Double = {
    val sumOfPriorities: Double = map.foldLeft(0.0)(_ + _._2)
    sumOfPriorities / map.size.toDouble
  }

}
