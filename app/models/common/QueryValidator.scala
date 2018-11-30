package models.common

import java.util.UUID

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging

import scala.collection.immutable.Map

case class ValidatedQuery(file: FileIdent, params: Map[String, String])

object QueryValidator extends LazyLogging {

  def apply(path: String, query: Map[String, Seq[String]]): Option[ValidatedQuery] = {
    for {
      fileId <- getUuidValue(query, "file_id")
      evidenceId <- getUuidValue(query, "evidence_id")
      partnerId <- getUuidValue(query, "partner_id")
      queryMap <- filterQueryForPath(path, query)
    } yield ValidatedQuery(FileIdent(fileId, evidenceId, partnerId), queryMap)
  }

  private def getUuidValue(query: Map[String, Seq[String]], key: String): Option[UUID] = {
    for {
      seq <- query.get(key)
      value <- seq.headOption
      uuid <- Convert.tryToUuid(value)
    } yield uuid
  }

  private def filterQueryForPath(path: String, query: Map[String, Seq[String]]): Option[Map[String, String]] = {
    if (path.startsWith("/media/hls/master")) {
      filterAllowedParams(query, whitelistedParams(RequestPathEnum.HlsCommon) ++ whitelistedParams(RequestPathEnum.HlsMaster))
    } else if (path.startsWith("/media/hls/variant")) {
      filterAllowedParams(query, whitelistedParams(RequestPathEnum.HlsCommon) ++ whitelistedParams(RequestPathEnum.HlsVariant))
    } else if (path.startsWith("/media/hls/segment")) {
      filterAllowedParams(query, whitelistedParams(RequestPathEnum.HlsCommon) ++ whitelistedParams(RequestPathEnum.HlsSegment))
    } else {
      logger.error("unexpectedQueryPath")("path" -> path)
      None
    }
  }

  private def filterAllowedParams(query: Map[String, Seq[String]], allowedParams: List[String]): Option[Map[String, String]] = {
    val filteredParams = query.filterKeys(allowedParams.contains(_))
    val result = filteredParams map { case (k, v) => k -> v.head }
    Some(result)
  }

  private val whitelistedParams = Map(
    RequestPathEnum.HlsCommon -> List(
      "offset",
      "selectaudio",
      "customlayout",
      "totalwidth",
      "totalheight",
      "t", "l", "w", "h",
      "fast",
      "dants",
      "disablebframes",
      "autorotate"
    ),
    RequestPathEnum.HlsMaster -> List(),
    RequestPathEnum.HlsVariant -> List("level"),
    RequestPathEnum.HlsSegment -> List("level", "index", "boost")
  )

  object RequestPathEnum extends Enumeration {
    val Thumbnail, HlsMaster, HlsVariant, HlsSegment, HlsCommon = Value
  }

}
