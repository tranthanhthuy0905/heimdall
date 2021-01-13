package controllers.v2

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction}
import javax.inject.Inject
import models.auth.StreamingSessionData
import play.api.mvc.ControllerComponents
import services.audit.AuditClient
import services.queue.ProbeNotifier
import services.rtm.RtmClient

import scala.concurrent.ExecutionContext

/**
 * Since RTMv2 keep the same API contract with RTMv1 so this Controller will extend itself in v1.
 * @param heimdallRequestAction
 * @param permValidation
 * @param rtmRequestAction
 * @param rtm
 * @param sessionData
 * @param audit
 * @param components
 * @param ex
 */
class ProbeController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  sessionData: StreamingSessionData,
  audit: AuditClient,
  components: ControllerComponents,
  notifier: ProbeNotifier
)(implicit ex: ExecutionContext)
    extends controllers.v1.ProbeController(heimdallRequestAction, permValidation, rtmRequestAction, rtm, sessionData, audit, components, notifier) {

}
