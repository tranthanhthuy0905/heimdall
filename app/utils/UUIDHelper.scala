package utils

import java.util.UUID

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging

import scala.collection.immutable.Map

trait UUIDHelper extends LazyLogging {

  def getUuidListByKey(key: String, map: Map[String, Seq[String]]): Option[List[UUID]] = {
    val uuidList = for {
      seq <- map.get(key)
      value <- seq.headOption
      uuidList <- this.parseStringToUuidList(value)
    } yield uuidList

    uuidList match {
      case Some(list) =>
        Some(list)
      case None =>
        logger.warn("failedToParseUUIDsForParam")(
          "map" -> map,
          "queryParam" -> key
        )
        None
    }
  }

  def getUuidValueByKey(key: String, query: Map[String, Seq[String]]): Option[UUID] = {
    for {
      seq <- query.get(key)
      value <- seq.headOption
      uuid <- Convert.tryToUuid(value)
    } yield uuid
  }

  def parseStringToUuidList(value: String): Option[List[UUID]] = {
    val strUuidList = value.split(",").toList
    val uuidList = strUuidList.map(uuidStr => Convert.tryToUuid(uuidStr)).collect {
      case Some(uuid) => uuid
    }
    if (uuidList.length == strUuidList.length) {
      Some(uuidList)
    } else {
      None
    }
  }

}
