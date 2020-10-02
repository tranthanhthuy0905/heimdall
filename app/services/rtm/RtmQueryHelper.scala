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
  final val hlsLongSegment    = "/media/hls/longsegment"
  final val thumbnail         = "/media/thumbnail"
  final val downloadThumbnail = "/media/downloadthumbnail"
  final val mp3               = "/media/audio/mp3"
  final val audioSample       = "/media/audio/sample"
  // RTMv2
  final val probeV2             = "/v2/media/start"
  final val hlsMasterV2         = "/v2/media/hls/master"
  final val hlsVariantV2        = "/v2/media/hls/variant"
  final val hlsSegmentV2        = "/v2/media/hls/segment"
  final val hlsLongSegmentV2    = "/v2/media/hls/longsegment"
  final val thumbnailV2         = "/v2/media/thumbnail"
  final val downloadThumbnailV2 = "/v2/media/downloadthumbnail"
  final val mp3V2               = "/v2/media/audio/mp3"
  final val audioSampleV2       = "/v2/media/audio/sample"
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
    "segmentVer",
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

  final val audioSampleParams = List(
    "startTimeSeconds",
    "endTimeSeconds",
    "resolutionSeconds"
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
    hlsVariant -> MediaRoute("/hls/variant", List.concat(commonParams, hlsVariantParams)),
    hlsSegment -> MediaRoute("/hls/segment", List.concat(commonParams, hlsSegmentParams)),
    hlsLongSegment -> MediaRoute("/hls/longsegment", List.concat(commonParams, hlsSegmentParams)),
    thumbnail -> MediaRoute("/thumbnail", List.concat(commonParams, thumbnailParams)),
    //thumbnail and downloadthumbnail process the same way but the second one emits an audit event.
    downloadThumbnail -> MediaRoute("/thumbnail", List.concat(commonParams, thumbnailParams)),
    audioSample -> MediaRoute("/audiosample", List.concat(commonParams, audioSampleParams)),
    mp3 -> MediaRoute("/mp3", List.empty),

    // RTMv2
    probeV2     -> MediaRoute("/probe", commonParams),
    hlsMasterV2 -> MediaRoute("/hls/master", commonParams),
    hlsVariantV2 -> MediaRoute("/hls/variant", List.concat(commonParams, hlsVariantParams)),
    hlsSegmentV2 -> MediaRoute("/hls/segment", List.concat(commonParams, hlsSegmentParams)),
    hlsLongSegmentV2 -> MediaRoute("/hls/longsegment", List.concat(commonParams, hlsSegmentParams)),
    thumbnailV2 -> MediaRoute("/thumbnail", List.concat(commonParams, thumbnailParams)),
    downloadThumbnailV2 -> MediaRoute("/thumbnail", List.concat(commonParams, thumbnailParams)),
    audioSampleV2 -> MediaRoute("/audiosample", List.concat(commonParams, audioSampleParams)),
    mp3V2 -> MediaRoute("/mp3", List.empty)
  )

  def apply(route: String, query: Map[String, Seq[String]]): Option[RtmQueryParams] = {
    route match {
      case str if str startsWith hlsMaster =>
        filterAllowedParams(query, heimdallToRtmRoutes(hlsMaster))
      case str if str startsWith hlsVariant =>
        filterAllowedParams(query, heimdallToRtmRoutes(hlsVariant))
      case str if str startsWith hlsSegment =>
        filterAllowedParams(query, heimdallToRtmRoutes(hlsSegment))
      case str if str startsWith hlsLongSegment =>
        filterAllowedParams(query, heimdallToRtmRoutes(hlsLongSegment))
      case str if str startsWith probe =>
        filterAllowedParams(query, heimdallToRtmRoutes(probe))
      case str if str startsWith thumbnail =>
        filterAllowedParams(query, heimdallToRtmRoutes(thumbnail))
      case str if str startsWith downloadThumbnail =>
        filterAllowedParams(query, heimdallToRtmRoutes(downloadThumbnail))
      case str if str startsWith audioSample =>
        filterAllowedParams(query, heimdallToRtmRoutes(audioSample))
      case str if str startsWith mp3 =>
        filterAllowedParams(query, heimdallToRtmRoutes(mp3))
      case str if str startsWith hlsMasterV2 =>
        filterAllowedParams(query, heimdallToRtmRoutes(hlsMasterV2))
      case str if str startsWith hlsVariantV2 =>
        filterAllowedParams(query, heimdallToRtmRoutes(hlsVariantV2))
      case str if str startsWith hlsSegmentV2 =>
        filterAllowedParams(query, heimdallToRtmRoutes(hlsSegmentV2))
      case str if str startsWith hlsLongSegmentV2 =>
        filterAllowedParams(query, heimdallToRtmRoutes(hlsLongSegmentV2))
      case str if str startsWith probeV2 =>
        filterAllowedParams(query, heimdallToRtmRoutes(probeV2))
      case str if str startsWith thumbnailV2 =>
        filterAllowedParams(query, heimdallToRtmRoutes(thumbnailV2))
      case str if str startsWith downloadThumbnailV2 =>
        filterAllowedParams(query, heimdallToRtmRoutes(downloadThumbnailV2))
      case str if str startsWith audioSampleV2 =>
        filterAllowedParams(query, heimdallToRtmRoutes(audioSampleV2))
      case str if str startsWith mp3V2 =>
        filterAllowedParams(query, heimdallToRtmRoutes(mp3V2))
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
