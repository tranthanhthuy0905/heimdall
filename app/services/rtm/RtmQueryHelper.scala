package services.rtm

import java.net.{URL, URLEncoder}

import com.evidence.service.common.logging.LazyLogging
import services.komrade.PlaybackSettings
import utils.UUIDHelper

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

  final val probeAll          ="/media/group/start"
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
    "streamToken",
    "lp",
    "ver", // support non-backward change in backend
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

  final val multiAudioStreamParams = List("multi", "stream", "sIndex")

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
    mp3 -> MediaRoute("/mp3", List.empty)
  )

  def apply(route: String, query: Map[String, Seq[String]], multiStreamEnabled: Boolean = false): Option[RtmQueryParams] = {
    route match {
      case str if str startsWith hlsMaster =>
        filterAllowedParams(query + ("multi" -> Seq(multiStreamEnabled.toString)), addMultiAudioParamsIfNeeded(heimdallToRtmRoutes(hlsMaster), multiStreamEnabled))
      case str if str startsWith hlsVariant =>
        filterAllowedParams(query, addMultiAudioParamsIfNeeded(heimdallToRtmRoutes(hlsVariant), multiStreamEnabled))
      case str if str startsWith hlsSegment =>
        filterAllowedParams(query, addMultiAudioParamsIfNeeded(heimdallToRtmRoutes(hlsSegment), multiStreamEnabled))
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
      case str if str startsWith probeAll =>
        filterAllowedParams(query, heimdallToRtmRoutes(probe))
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

  def getRTMQueries(
                             queries: Map[String, String],
                             watermark: Option[String],
                             playbackSettings: Option[PlaybackSettings],
                             presignedUrls: Seq[URL],
                             partnerId: String): String = {
    val presignedUrlsMap = Map("source" -> presignedUrls.mkString(","))
    val watermarkMap = watermark.map(watermark => queries + ("label" -> watermark)).getOrElse(Map.empty)
    val playbackSettingsMap = playbackSettings.map(_.toMap).getOrElse(Map.empty)
    buildQueryParams(queries ++ presignedUrlsMap ++ watermarkMap ++ playbackSettingsMap)
  }

  private def buildQueryParams(map: Map[String, String]): String = {
    map.toSeq.map {
      case (key, value) =>
        URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(
          value,
          "UTF-8"
        )
    }.reduceLeft(_ + "&" + _)
  }

  private def addMultiAudioParamsIfNeeded(route: MediaRoute, multiAudioEnabled: Boolean): MediaRoute = {
    if (multiAudioEnabled) {
      MediaRoute(route.rtmPath, List.concat(route.whitelistedParams, multiAudioStreamParams))
    } else {
      route
    }
  }

  case class MediaRoute(rtmPath: String, whitelistedParams: List[String])
}
