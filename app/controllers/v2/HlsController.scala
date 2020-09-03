package controllers.v2

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction, TokenValidationAction, WatermarkAction}
import com.typesafe.config.Config
import javax.inject.Inject
import play.api.mvc.ControllerComponents
import services.rtm.RtmClient

import scala.concurrent.ExecutionContext

/**
 * Since RTMv2 keep the same API contract with RTMv1 so this Controller will extend itself in v1.
 * @param heimdallRequestAction
 * @param tokenValidationAction
 * @param permValidation
 * @param watermarkAction
 * @param rtmRequestAction
 * @param rtm
 * @param config
 * @param components
 * @param ex
 */
class HlsController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  watermarkAction: WatermarkAction,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  config: Config,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends controllers.v1.HlsController(heimdallRequestAction, tokenValidationAction, permValidation, watermarkAction, rtmRequestAction, rtm, components) {
}
