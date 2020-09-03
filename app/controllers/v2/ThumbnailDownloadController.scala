package controllers.v2

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction}
import javax.inject.Inject
import play.api.mvc.ControllerComponents
import services.audit.AuditClient
import services.rtm.RtmClient

import scala.concurrent.ExecutionContext

/**
 * Since RTMv2 keep the same API contract with RTMv1 so this Controller will extend itself in v1.
 * @param heimdallRequestAction
 * @param permValidation
 * @param rtmRequestAction
 * @param rtm
 * @param audit
 * @param components
 * @param ex
 */
class ThumbnailDownloadController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  audit: AuditClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends controllers.v1.ThumbnailDownloadController(heimdallRequestAction, permValidation, rtmRequestAction, rtm, audit, components) {

}
