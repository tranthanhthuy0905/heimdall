package controllers

import actions.{HeimdallRequestAction, PlaybackSettingAction, RtmRequestAction }
import com.evidence.service.common.monad.FutureEither
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import com.typesafe.config.Config
import models.common.{HeimdallRequest}
import models.hls.HlsManifestFormatter
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.rtm.{RtmClient}
import utils.{HdlResponseHelpers, WSResponseHelpers}

import javax.inject.Inject

import scala.concurrent.ExecutionContext

class HlsDebugController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  playbackSettingAction: PlaybackSettingAction,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  config: Config,
  components: ControllerComponents
  )(implicit ex: ExecutionContext)
  extends AbstractController(components)
  with WSResponseHelpers
  with HdlResponseHelpers
  with StrictStatsD {

    def master: Action[AnyContent] =
      (
        heimdallRequestAction andThen
          playbackSettingAction andThen
          rtmRequestAction
        ).async { request =>
        FutureEither(rtm.send(request).map(withOKStatus))
          .map(toManifest(_, request))
          .fold(error, Ok(_).as("application/x-mpegURL"))
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



