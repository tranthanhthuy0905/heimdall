package services.rtm

import com.evidence.service.common.logging.LazyLogging
import utils.UUIDHelper

import scala.collection.immutable.Map

/**
  * RtmQueryParams is a query extended with file identifier.
  * It contains RTM path/route/API, and validated and filtered query parameters.
  * I.e. needed values to perform a RTM request.
  */
case class RtmQueryParams(path: String, params: Map[String, String])

trait HeimdallRoutes {
  final val probe             = "/media/start"
  final val hlsMaster         = "/media/hls/master"
  final val hlsVariant        = "/media/hls/variant"
  final val hlsSegment        = "/media/hls/segment"
  final val thumbnail         = "/media/thumbnail"
  final val downloadThumbnail = "/media/downloadthumbnail"
  final val mp3               = "/media/mp3"
}

object RtmQueryHelper extends LazyLogging with HeimdallRoutes with UUIDHelper {

  /**
    * TODO: Heimdall and RTM are case sensitive. This may cause issues with processing of requests.
    */
  final val commonParams = List(
    "offset",
    "selectaudio",
    "customlayout",
    "totalwidth",
    "totalheight",
    "t",
    "l",
    "w",
    "h",
    "fast",
    "dants",
    "disableBFrames",
    "autorotate",
    "autoFixPTS",
    "streamingSessionToken",
    "streamToken"
  )

  final val hlsVariantParams = List("level")

  final val hlsSegmentParams = List("level", "index", "boost")

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
    probe     -> MediaRoute("/probe", commonParams),
    hlsMaster -> MediaRoute("/hls/master", commonParams),
    hlsVariant -> MediaRoute(
      "/hls/variant",
      List.concat(commonParams, hlsVariantParams)
    ),
    hlsSegment -> MediaRoute(
      "/hls/segment",
      List.concat(commonParams, hlsSegmentParams)
    ),
    thumbnail -> MediaRoute(
      "/thumbnail",
      List.concat(commonParams, thumbnailParams)
    ),
    /**
      * RTM processes both thumbnail and downloadthumbnail the same way.
      * The difference between thumbnail and downloadthumbnail is that the second one emits an audit event.
      */
    downloadThumbnail -> MediaRoute(
      "/thumbnail",
      List.concat(commonParams, thumbnailParams)
    ),
  )

  def apply(route: String, query: Map[String, Seq[String]]): Option[RtmQueryParams] = {
    route match {
      case str if str startsWith hlsMaster =>
        filterAllowedParams(query, heimdallToRtmRoutes(hlsMaster))
      case str if str startsWith hlsVariant =>
        filterAllowedParams(query, heimdallToRtmRoutes(hlsVariant))
      case str if str startsWith hlsSegment =>
        filterAllowedParams(query, heimdallToRtmRoutes(hlsSegment))
      case str if str startsWith probe =>
        filterAllowedParams(query, heimdallToRtmRoutes(probe))
      case str if str startsWith thumbnail =>
        filterAllowedParams(query, heimdallToRtmRoutes(thumbnail))
      case str if str startsWith downloadThumbnail =>
        filterAllowedParams(query, heimdallToRtmRoutes(downloadThumbnail))
      case _ =>
        logger.error("unexpectedRtmQueryRoute")(
          "route" -> route,
          "query" -> query
        )
        None
    }
  }

  private def filterAllowedParams(
    query: Map[String, Seq[String]],
    mediaRoute: MediaRoute
  ): Option[RtmQueryParams] = {
    // TODO: Converting strings to lower case can be implemented here.
    val filteredParams = query
      .filterKeys(mediaRoute.whitelistedParams.contains(_))
      .filter(_._2.nonEmpty)
      .map { case (k, v) => k -> v.headOption.getOrElse("") }

    val result = RtmQueryParams(mediaRoute.rtmPath, filteredParams)
    logger.debug("filterAllowedParamsResult")(
      "originalQuery"       -> query,
      "mediaRoute"          -> mediaRoute,
      "filteredQueryParams" -> result
    )

    Some(result)
  }

  case class MediaRoute(rtmPath: String, whitelistedParams: List[String])
}
