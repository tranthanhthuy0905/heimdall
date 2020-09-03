package controllers.v2

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction, TokenValidationAction}
import javax.inject.Inject
import models.common.PermissionType
import play.api.http.HttpEntity
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.rtm.{RtmClient, RtmResponseHandler}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Since RTMv2 keep the same API contract with RTMv1 so this Controller will extend itself in v1
 * @param heimdallRequestAction
 * @param tokenValidationAction
 * @param permValidation
 * @param rtmRequestAction
 * @param rtm
 * @param components
 * @param ex
 */
class AudioController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends controllers.v1.AudioController(heimdallRequestAction, tokenValidationAction, permValidation, rtmRequestAction, rtm, components) {

}
