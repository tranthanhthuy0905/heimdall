package controllers

import actions.{
  HeimdallRequestAction,
  PermValidationActionBuilder,
  RtmRequestAction,
  TokenValidationAction
}
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.PermissionType
import models.hls.HlsManifestFormatter
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import scala.concurrent.ExecutionContext
import services.rtm.{RtmClient, RtmResponseHandler}


class AudioController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components) {

  def sample: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        tokenValidationAction andThen
        permValidation.build(PermissionType.View) andThen
        rtmRequestAction
    ).async { request =>
      val okCallback = (response: WSResponse) => {
        val contentType = response.headers
          .get("Content-Type")
          .flatMap(_.headOption)
          .getOrElse("application/json")
        Ok(response.json).as(contentType)
      }

      rtm.send(request.toString) map { response =>
        RtmResponseHandler(
          response,
          okCallback,
          Seq[(String, Any)](
            "path"       -> request.path,
            "mediaIdent" -> request.media,
            "status"     -> response.status,
            "message"    -> response.body
          )
        )
      }

    }
}
