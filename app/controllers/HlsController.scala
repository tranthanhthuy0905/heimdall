package controllers

import actions._
import com.evidence.service.common.monad.FutureEither
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import javax.inject.Inject
import models.common.{HeimdallRequest, PermissionType}
import models.hls.HlsManifestFormatter
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.rtm.{RtmClient, RtmRequest}
import com.typesafe.config.Config
import utils.{HdlResponseHelpers, WSResponseHelpers}

import scala.concurrent.ExecutionContext

class HlsController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  playbackSettingAction: PlaybackSettingAction,
  watermarkAction: WatermarkAction,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  config: Config,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with WSResponseHelpers
    with HdlResponseHelpers
    with StrictStatsD {

  def playlist: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        tokenValidationAction andThen
        permValidation.build(PermissionType.Stream) andThen
        playbackSettingAction andThen
        rtmRequestAction
    ).async { request =>
      FutureEither(rtm.send(request).map(withOKStatus))
        .map(toManifest(_, request))
        .fold(error, Ok(_).as("application/x-mpegURL"))
    }

  def segment: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        tokenValidationAction andThen
        permValidation.build(PermissionType.Stream) andThen
        watermarkAction andThen
        rtmRequestAction
    ).async { request: RtmRequest[AnyContent] =>
        FutureEither(rtm.send(request).map(withOKStatus))
          .fold(error, streamed(_, "video/MP2T"))
    }

  private def toManifest(response: WSResponse, request: HeimdallRequest[AnyContent]): String = {
    HlsManifestFormatter(
      response.body,
      request.media,
      config.getString("heimdall.api_prefix"),
      Some(request.streamingSessionToken)
    )
  }
}
