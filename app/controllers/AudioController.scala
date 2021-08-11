package controllers

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction, TokenValidationAction}
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common.PermissionType
import play.api.http.ContentTypes
import play.api.mvc._
import services.rtm.RtmClient
import utils.{HdlResponseHelpers, WSResponseHelpers}

import scala.concurrent.ExecutionContext

class AudioController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with WSResponseHelpers
    with HdlResponseHelpers {

  def sample: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        rtmRequestAction
    ).async { request =>
      FutureEither(rtm.send(request).map(withOKStatus))
        .fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }

  def mp3: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        rtmRequestAction
    ).async { implicit request =>
      FutureEither(rtm.send(request).map(withOKStatus))
        .fold(error, streamedSuccessResponse(_, "audio/mpeg"))
    }
}
