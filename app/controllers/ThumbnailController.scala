package controllers

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction, WatermarkAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common.PermissionType
import play.api.mvc._
import services.rtm.RtmClient
import utils.{HdlResponseHelpers, WSResponseHelpers}

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
    with LazyLogging
    with WSResponseHelpers
    with HdlResponseHelpers {

  def thumbnail: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        watermarkAction andThen
        rtmRequestAction
    ).async { request =>
      FutureEither(rtm.send(request).map(withOKStatus))
        .fold(error, streamed(_, "image/jpeg"))
    }
}
