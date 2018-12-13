package models.common

import java.util.UUID

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging

import scala.collection.immutable.Map

/**
  * RtmQueryParams is a query extended with file identifier.
  * It contains RTM path/route/API, and validated and filtered query parameters.
  * I.e. needed values to perform a RTM request.
  */
case class RtmQueryParams(file: FileIdent, path: String, params: Map[String, String])

trait HeimdallRoutes {
  final val Health = "/media/alive"
  final val Probe = "/media/start"
  final val HlsMaster = "/media/hls/master"
  final val HlsVariant = "/media/hls/variant"
  final val HlsSegment = "/media/hls/segment"
  final val Thumbnail = "/media/thumbnail"
  final val Mp3 = "/media/mp3"
}

object QueryHelper extends LazyLogging with HeimdallRoutes {

  case class MediaRoute(rtmPath: String, whitelistedParams: List[String])

  case class QueryWithPath(rtmPath: String, params: Map[String, String])

  private final val commonParams = List(
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

  private final val hlsVariantParams = List(
    "level"
  )

  private final val hlsSegmentParams = List(
    "level",
    "index",
    "boost"
  )

  /**
    * heimdallToRtmRoutes provides Heimdall to RTM routes mapping ,and a list of whitelisted parameters per RTM route.
    *
    * The media routes below have to be in sync with RTM's registered paths.
    * See:
    * https://git.taservs.net/ecom/rtm/blob/cae8381f91b668eaed143cfe07dd6fa4a8acf6ba/src/rtm/server/http/server.go#L220
    *
    * Note: /flv was deprecated. Thus, Heimdall does not support it.
    */
  private final val heimdallToRtmRoutes = Map(
    Health -> MediaRoute("/health", List()),
    Probe -> MediaRoute("/probe", commonParams),
    HlsMaster -> MediaRoute("/hls/master", commonParams),
    HlsVariant -> MediaRoute("/hls/variant", List.concat(commonParams, hlsVariantParams)),
    HlsSegment -> MediaRoute("/hls/segment", List.concat(commonParams, hlsSegmentParams))
    // TODO: "/media/thumbnail", "/media/mp3"
  )

  def apply(route: String, query: Map[String, Seq[String]]): Option[RtmQueryParams] = {
    for {
      fileId <- getUuidValue(query, "file_id")
      evidenceId <- getUuidValue(query, "evidence_id")
      partnerId <- getUuidValue(query, "partner_id")
      query <- filterQueryForRoute(route, query)
    } yield RtmQueryParams(FileIdent(fileId, evidenceId, partnerId), query.rtmPath, query.params)
  }

  private def getUuidValue(query: Map[String, Seq[String]], key: String): Option[UUID] = {
    for {
      seq <- query.get(key)
      value <- seq.headOption
      uuid <- Convert.tryToUuid(value)
    } yield uuid
  }

  private def filterQueryForRoute(route: String, query: Map[String, Seq[String]]): Option[QueryWithPath] = {
    if (route.startsWith(HlsMaster)) {
      Some(filterAllowedParams(query, heimdallToRtmRoutes(HlsMaster)))
    } else if (route.startsWith(HlsVariant)) {
      Some(filterAllowedParams(query, heimdallToRtmRoutes(HlsVariant)))
    } else if (route.startsWith(HlsSegment)) {
      Some(filterAllowedParams(query, heimdallToRtmRoutes(HlsSegment)))
    } else if (route.startsWith(Probe)) {
      Some(filterAllowedParams(query, heimdallToRtmRoutes(Probe)))
    } else {
      logger.error("unexpectedQueryRoute")("route" -> route)
      None
    }
  }

  private def filterAllowedParams(query: Map[String, Seq[String]], mediaRoute: MediaRoute): QueryWithPath = {
    val filteredParams = query.filterKeys(mediaRoute.whitelistedParams.contains(_))
    val result = {
      filteredParams map { case (k, v) => k -> v.head }
    }
    QueryWithPath(mediaRoute.rtmPath, result)
  }

}
