package utils

import java.util.UUID

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging

object UUIDHelper extends LazyLogging {

  def parseStringToUuidList(value: String): Option[List[UUID]] = {
    val strUuidList = value.split(",").toList
    val uuidList = strUuidList.map(uuidStr => Convert.tryToUuid(uuidStr)).collect {
      case Some(uuid) => uuid
    }
    if (uuidList.length == strUuidList.length) {
      Some(uuidList)
    } else {
      logger.error("failedToParseStringValueToUuidList")("value" -> value)
      None
    }
  }

}
