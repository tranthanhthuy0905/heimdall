package services.zookeeper

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.zookeeper.ServiceEndpoint
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.{
  ChildData,
  PathChildrenCache,
  PathChildrenCacheEvent,
  PathChildrenCacheListener
}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

/**
  * ZookeeperNodeCache extends Curator's PathChildrenCache.
  * A utility that attempts to keep all data from all children of a ZK path locally cached.
  * This class will watch the ZK path, respond to update/create/delete events, pull down the data, etc.
  *
  * @param nodeCache A utility that attempts to keep all data from all children of a ZK path locally cached.
  *                  NodeCache will watch the ZK path, respond to update/create/delete events,
  *                  pull down the data, etc.
  * @param listenerCallback is a callback triggered on zookeeper node updates.
  */
abstract class ZookeeperNodeCache[T](nodeCache: PathChildrenCacheFacade, listenerCallback: () => Unit)
    extends LazyLogging {

  private[this] val cacheListener: ZookeeperCacheListener = new ZookeeperCacheListener(listenerCallback)
  private[this] val cache: PathChildrenCacheFacade        = nodeCache

  def start(): Unit = {
    logger.info("startingZookeeperTreeCache")()
    cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE)

    logger.info("addingTreeCacheListener")()
    cache.getListenable.addListener(cacheListener)
  }

  def stop(): Unit = {
    logger.info("removingZookeeperCacheListener")()
    Try(cache.getListenable.removeListener(cacheListener))

    logger.info("shuttingDownTreeCache")()
    Try(cache.close())
  }

  def getData: List[T] = {
    withZookeeperNodeCache { cache =>
      val currentData = cache.getCurrentData
      currentData
    }.fold {
      List.empty[T]
    } { data: java.util.List[ChildData] =>
      data.asScala.map(cd => transform(cd)).toList.flatten
    }
  }

  def transform(childData: ChildData): Option[T]

  private[this] def withZookeeperNodeCache[K](fn: PathChildrenCacheFacade => K): Option[K] = {
    Option(fn(cache))
  }

}

class RtmZookeeperNodeCache(nodeCache: PathChildrenCacheFacade, listenerCallback: () => Unit)
    extends ZookeeperNodeCache[ServiceEndpoint](nodeCache, listenerCallback) {

  def transform(childData: ChildData): Option[ServiceEndpoint] = {
    val path = childData.getPath.split("/")
    if (path.last.startsWith("member_")) {
      val childDataString = new String(childData.getData, "UTF-8")
      Try(Json.parse(childDataString).as[RtmNodeInfo]) match {
        case Success(rtmNodeData) =>
          if (rtmNodeData.isAlive) {
            Some(rtmNodeData.serviceEndpoint)
          } else {
            logger.warn("rtmNodeIsNotActive")(
              "path"            -> childData.getPath,
              "childDataString" -> childDataString
            )
            None
          }
        case _ =>
          logger.warn("failedToParseRtmNodeInfo")(
            "path"            -> childData.getPath,
            "childDataString" -> childDataString
          )
          None
      }
    } else {
      logger.debug("foundPerftrakNode")("path" -> childData.getPath)
      None
    }
  }

}

class PerftrakZookeeperNodeCache(nodeCache: PathChildrenCacheFacade, listenerCallback: () => Unit)
    extends ZookeeperNodeCache[PerftrakDatum](nodeCache, listenerCallback) {

  def transform(childData: ChildData): Option[PerftrakDatum] = {
    val childDataString    = new String(childData.getData, "UTF-8")
    val jsonData: JsValue  = Json.parse(childDataString)
    val planeComputational = getPlaneComputational(jsonData)
    val planeCaching       = getPlaneCaching(jsonData)
    getEndpointString(childData) match {
      case Some(v) =>
        Some(PerftrakDatum(v, planeComputational, planeCaching))
      case _ =>
        None
    }
  }

  private def getPlaneComputational(jsonData: JsValue): Option[PlaneComputational] = {
    val planeComputational: Option[PlaneComputational] = PerftrakModel.planeComputationalReads.reads(jsonData) match {
      case parsedData: JsSuccess[PlaneComputational] => Some(parsedData.value)
      case e: JsError =>
        logger.error("failedToParsePlaneComputationalPerftrakData")(
          "message" -> JsError.toJson(e).toString(),
          "json"    -> jsonData.toString()
        )
        None
    }
    planeComputational
  }

  private def getPlaneCaching(jsonData: JsValue): Option[PlaneCaching] = {
    val planeCaching: Option[PlaneCaching] = PerftrakModel.planeCachingReads.reads(jsonData) match {
      case parsedData: JsSuccess[Option[PlaneCaching]] => parsedData.value
      case e: JsError =>
        logger.info("failedToParsePlaneCachingPerftrakData")(
          "message" -> JsError.toJson(e).toString(),
          "json"    -> jsonData.toString()
        )
        None
    }
    planeCaching
  }

  private def getEndpointString(childData: ChildData): Option[ServiceEndpoint] = {
    val path     = childData.getPath.split("/").last.split(":")
    val endpoint = ServiceEndpoint(path.head, path.last.toInt)
    if (endpoint.host.length > 0 && endpoint.port > 0) {
      Some(endpoint)
    } else {
      None
    }
  }
}

/**
  * A listener callback class that receives messages on changes in zNodes.
  *
  * @param callback is a callback triggered on zookeeper node update.
  */
class ZookeeperCacheListener(callback: () => Unit) extends PathChildrenCacheListener with LazyLogging {
  override def childEvent(client: CuratorFramework, event: PathChildrenCacheEvent) {
    event.getType match {
      case PathChildrenCacheEvent.Type.CONNECTION_SUSPENDED | PathChildrenCacheEvent.Type.CONNECTION_LOST =>
        logger.error("lostConnectionWithNode")("data" -> event.getData)
      case _ =>
        callback()
    }
  }
}
