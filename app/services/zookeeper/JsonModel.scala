package services.zookeeper

import com.evidence.service.common.zookeeper.ServiceEndpoint
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import scala.collection.immutable.Seq

/**
  * Example of rtm node info:
  * {
  *   "serviceEndpoint": {
  *     "host":"qus1uw2lrtm002.taservs.net",
  *     "port":8900
  *   },
  *   "additionalEndpoints":{},
  *   "status":"ALIVE"
  * }
  */
case class RtmNodeInfo(serviceEndpoint: ServiceEndpoint, status: String) {
  def isAlive: Boolean = status.toLowerCase == "alive"
}

object RtmNodeInfo {
  implicit val serviceEndpointReads: Reads[ServiceEndpoint] = Json.reads[ServiceEndpoint]
  implicit val rtmNodeDataReads: Reads[RtmNodeInfo]         = Json.reads[RtmNodeInfo]
}

/**
  * Example of perftrak data:
  *
  * {
  *   "plane-caching": {
  *     "component-tops": [                     <<<<< Tops
  *       {
  *         "k": "9ab207374d354948866bc52280e64098",
  *         "v": 206045180
  *       },
  *       {
  *         "k": "59e39a0a88de4f0c8548d9697de51fa6",
  *         "v": 134217730
  *       },
  *       ...
  *       {
  *         "k": "48454df382724423b73c7bb65c5e58ed",
  *         "v": 4718592
  *       }
  *     ]
  *   },
  *   "plane-computational": {                  <<<<< PlaneComputational
  *     "component-aggregate": 0
  *   }
  * }
  *
  */
case class SingleTop(k: String, v: Double)

/**
  * PlaneCaching wraps needed value from the plane-caching published by RTM.
  *
  * @param tops stores value retrieved from perftrak plane-caching → component-tops.
  *             The component stores 50 top contributing entities (file IDs) for current RTM node.
  *             Entity ID is file ID, and value amount of memory in bytes.
  *             See https://git.taservs.net/ecom/rtm-common/blob/master/mio/range/cacher/cacher_instance.go#L316
  */
case class PlaneCaching(tops: Seq[SingleTop])

/**
  * PlaneComputational wraps plane-computational values published by RTM.
  *
  * @param aggregate stores value retrieved from perftrak plane-computational → component-aggregate.
  *                  value must be normalized to 1
  */
case class PlaneComputational(aggregate: Double)

object PerftrakModel {
  implicit val singleTopReads: Reads[SingleTop] = Json.reads[SingleTop]
  implicit val planeCachingReads: Reads[Option[PlaneCaching]] =
    (JsPath \ "plane-caching" \ "component-tops").readNullable[Seq[SingleTop]].map(_.map(PlaneCaching))
  implicit val planeComputationalReads: Reads[PlaneComputational] =
    (JsPath \ "plane-computational" \ "component-aggregate").read[Double].map(PlaneComputational.apply)
}
