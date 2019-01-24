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
case class RtmQueryParams(media: MediaIdent, path: String, params: Map[String, String])

trait HeimdallRoutes {
  final val health = "/media/alive"
  final val probe = "/media/start"
  final val hlsMaster = "/media/hls/master"
  final val hlsVariant = "/media/hls/variant"
  final val hlsSegment = "/media/hls/segment"
  final val thumbnail = "/media/thumbnail"
  final val mp3 = "/media/mp3"
  final val streamed = "/media/streamed"
}

object QueryHelper extends LazyLogging with HeimdallRoutes {

  case class MediaRoute(rtmPath: String, whitelistedParams: List[String])

  case class QueryWithPath(rtmPath: String, params: Map[String, String])

  final val commonParams = List(
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

  final val hlsVariantParams = List(
    "level"
  )

  final val hlsSegmentParams = List(
    "level",
    "index",
    "boost"
  )

  final val thumbnailParams = List(
    "time",
    "download",
    "width",
    "height",
    "left",
    "top",
    "right",
    "bottom"
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
    health -> MediaRoute("/health", List()),
    probe -> MediaRoute("/probe", commonParams),
    hlsMaster -> MediaRoute("/hls/master", commonParams),
    hlsVariant -> MediaRoute("/hls/variant", List.concat(commonParams, hlsVariantParams)),
    hlsSegment -> MediaRoute("/hls/segment", List.concat(commonParams, hlsSegmentParams)),
    thumbnail -> MediaRoute("/thumbnail", List.concat(commonParams, thumbnailParams)),
    streamed -> MediaRoute("", List())
  )

  def apply(route: String, query: Map[String, Seq[String]]): Option[RtmQueryParams] = {
    for {
      fileIds <- getUuidList(query, "file_id")
      evidenceIds <- getUuidList(query, "evidence_id")
      partnerId <- getUuidValue(query, "partner_id")
      query <- filterQueryForRoute(route, query)
    } yield RtmQueryParams(MediaIdent(fileIds, evidenceIds, partnerId), query.rtmPath, query.params)
  }

  private def getUuidList(query: Map[String, Seq[String]], key: String): Option[List[UUID]] = {
    for {
      seq <- query.get(key)
      value <- seq.headOption
      uuidList <- parseStringToUuidList(value)
    } yield uuidList
  }

  private def getUuidValue(query: Map[String, Seq[String]], key: String): Option[UUID] = {
    for {
      seq <- query.get(key)
      value <- seq.headOption
      uuid <- Convert.tryToUuid(value)
    } yield uuid
  }

  private def parseStringToUuidList(value: String): Option[List[UUID]] = {
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

  private def filterQueryForRoute(route: String, query: Map[String, Seq[String]]): Option[QueryWithPath] = {
    route match {
      case str if str startsWith hlsMaster => Some(filterAllowedParams(query, heimdallToRtmRoutes(hlsMaster)))
      case str if str startsWith hlsVariant => Some(filterAllowedParams(query, heimdallToRtmRoutes(hlsVariant)))
      case str if str startsWith hlsSegment => Some(filterAllowedParams(query, heimdallToRtmRoutes(hlsSegment)))
      case str if str startsWith probe => Some(filterAllowedParams(query, heimdallToRtmRoutes(probe)))
      case str if str startsWith thumbnail => Some(filterAllowedParams(query, heimdallToRtmRoutes(thumbnail)))
      case str if str startsWith streamed => Some(filterAllowedParams(query, heimdallToRtmRoutes(streamed)))
      case _ =>
        logger.error("unexpectedQueryRoute")("route" -> route)
        None
    }
  }

  private def filterAllowedParams(query: Map[String, Seq[String]], mediaRoute: MediaRoute): QueryWithPath = {
    val filteredParams = query.filterKeys(mediaRoute.whitelistedParams.contains(_)).filter(_._2.nonEmpty)
    val result = filteredParams.map { case (k, v) => k -> v.head }
    QueryWithPath(mediaRoute.rtmPath, result)
  }

}
