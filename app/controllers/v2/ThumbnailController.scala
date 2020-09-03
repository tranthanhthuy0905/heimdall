package controllers.v2

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction, WatermarkAction}
import javax.inject.Inject
import play.api.mvc.ControllerComponents
import services.rtm.RtmClient

import scala.concurrent.ExecutionContext

/**
 * Since RTMv2 keep the same API contract with RTMv1 so this Controller will extend itself in v1.
 * @param heimdallRequestAction
 * @param permValidation
 * @param watermarkAction
 * @param rtmRequestAction
 * @param rtm
 * @param components
 * @param ex
 */
class ThumbnailController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  watermarkAction: WatermarkAction,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends controllers.v1.ThumbnailController(heimdallRequestAction, permValidation, watermarkAction, rtmRequestAction, rtm, components) {

}
