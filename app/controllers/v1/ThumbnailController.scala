package controllers.v1

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction, WatermarkAction}
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.PermissionType
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.rtm.{RtmClient, RtmResponseHandler}

import scala.concurrent.ExecutionContext

class ThumbnailController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  watermarkAction: WatermarkAction,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging {

  def thumbnail: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        watermarkAction andThen
        rtmRequestAction
    ).async { request =>
      val okCallback = (response: WSResponse) => {
        val contentType = response.headers
          .get("Content-Type")
          .flatMap(_.headOption)
          .getOrElse("image/jpeg")
        Ok.chunked(response.bodyAsSource).as(contentType)
      }

      rtm.send(request) map { response =>
        RtmResponseHandler(
          response,
          okCallback,
          Seq[(String, Any)](
            "path"       -> request.path,
            "mediaIdent" -> request.media,
            "status"     -> response.status,
            "message"    -> response.body,
            "token"      -> request.streamingSessionToken
          )
        )
      }
    }

}
