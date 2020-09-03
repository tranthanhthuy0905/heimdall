package controllers.v1

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction, TokenValidationAction, WatermarkAction}
import javax.inject.Inject
import models.common.PermissionType
import models.hls.HlsManifestFormatter
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.rtm.{RtmClient, RtmResponseHandler}

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
    extends AbstractController(components) {

  def playlist: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        tokenValidationAction andThen
        permValidation.build(PermissionType.Stream) andThen
        rtmRequestAction
    ).async { request =>
      val okCallback = (response: WSResponse) => {
        val contentType = response.headers
          .get("Content-Type")
          .flatMap(_.headOption)
          .getOrElse("application/x-mpegURL")
        val newManifest =
          HlsManifestFormatter(
            response.body,
            request.media,
            request.apiPathPrefixForBuildingHlsManifest,
            Some(request.streamingSessionToken)
          )
        Ok(newManifest).as(contentType)
      }

      rtm.send(request) map { response =>
        RtmResponseHandler(
          response,
          okCallback,
          Seq[(String, Any)](
            "path"       -> request.path,
            "token"      -> request.streamingSessionToken,
            "mediaIdent" -> request.media,
            "status"     -> response.status,
            "message"    -> response.body
          )
        )
      }

    }

  def segment: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        tokenValidationAction andThen
        permValidation.build(PermissionType.Stream) andThen
        watermarkAction andThen
        rtmRequestAction
    ).async { request =>
      val okCallback = (response: WSResponse) => {
        val contentType = response.headers
          .get("Content-Type")
          .flatMap(_.headOption)
          .getOrElse("video/MP2T")
        Ok.chunked(response.bodyAsSource).as(contentType)
      }

      rtm.send(request) map { response =>
        RtmResponseHandler(
          response,
          okCallback,
          Seq[(String, Any)](
            "path"       -> request.path,
            "token"      -> request.streamingSessionToken,
            "mediaIdent" -> request.media,
            "status"     -> response.status,
            "message"    -> response.body
          )
        )
      }
    }
}
