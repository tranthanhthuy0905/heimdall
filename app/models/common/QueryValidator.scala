package models.common

import java.util.UUID

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging

import scala.collection.immutable.Map

trait HeimdallRoutes {
  protected final val Health = "/media/alive"
  protected final val Probe = "/media/start"
  protected final val HlsMaster = "/media/hls/master"
  protected final val HlsVariant = "/media/hls/variant"
  protected final val HlsSegment = "/media/hls/segment"
  protected final val Thumbnail = "/media/thumbnail"
  protected final val Mp3 = "/media/mp3"
}

case class ValidatedQuery(file: FileIdent, params: Map[String, String])

object QueryValidator extends LazyLogging with HeimdallRoutes {
  private final val CommonParams = List(
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
  )

  private final val HlsVariantParams = List(
    "level"
  )

  private final val HlsSegmentParams = List(
    "level",
    "index",
    "boost"
  )

  private final val WhitelistedParams = Map(
    Probe -> CommonParams,
    HlsMaster -> CommonParams,
    HlsVariant -> List.concat(CommonParams, HlsVariantParams),
    HlsSegment -> List.concat(CommonParams, HlsSegmentParams)
  )

  def apply(route: String, query: Map[String, Seq[String]]): Option[ValidatedQuery] = {
    for {
      fileId <- getUuidValue(query, "file_id")
      evidenceId <- getUuidValue(query, "evidence_id")
      partnerId <- getUuidValue(query, "partner_id")
      queryMap <- filterQueryForRoute(route, query)
    } yield ValidatedQuery(FileIdent(fileId, evidenceId, partnerId), queryMap)
  }

  private def getUuidValue(query: Map[String, Seq[String]], key: String): Option[UUID] = {
    for {
      seq <- query.get(key)
      value <- seq.headOption
      uuid <- Convert.tryToUuid(value)
    } yield uuid
  }

  private def filterQueryForRoute(route: String, query: Map[String, Seq[String]]): Option[Map[String, String]] = {
    if (route.startsWith(HlsMaster)) {
      filterAllowedParams(query, WhitelistedParams(HlsMaster))
    } else if (route.startsWith(HlsVariant)) {
      filterAllowedParams(query, WhitelistedParams(HlsVariant))
    } else if (route.startsWith(HlsSegment)) {
      filterAllowedParams(query, WhitelistedParams(HlsSegment))
    } else if (route.startsWith(Probe)) {
      filterAllowedParams(query, WhitelistedParams(Probe))
    } else {
      logger.error("unexpectedQueryRoute")("route" -> route)
      None
    }
  }

  private def filterAllowedParams(query: Map[String, Seq[String]], allowedParams: List[String]): Option[Map[String, String]] = {
    val filteredParams = query.filterKeys(allowedParams.contains(_))
    val result = {
      filteredParams map { case (k, v) => k -> v.head }
    }
    Some(result)
  }

}
