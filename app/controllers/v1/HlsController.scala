package controllers.v1

import actions._
import com.evidence.service.common.monad.FutureEither
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import javax.inject.Inject
import models.common.{HeimdallRequest, PermissionType}
import models.hls.HlsManifestFormatter
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.rtm.{RtmClient, RtmRequest}
import utils.{HdlResponseHelpers, WSResponseHelpers}

import scala.concurrent.ExecutionContext

class HlsController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  watermarkAction: WatermarkAction,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
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
      executionTime(
        "HlsController.segment",
        FutureEither(rtm.send(request).map(withOKStatus))
          .fold(error, streamed(_, "video/MP2T")),
        s"rtm:v${request.getOverrideRtmApiVersion}"
      )
    }

  private def toManifest(response: WSResponse, request: HeimdallRequest[AnyContent]): String = {
    HlsManifestFormatter(
      response.body,
      request.media,
      request.apiPathPrefixForBuildingHlsManifest,
      Some(request.streamingSessionToken)
    )
  }
}
