package controllers

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.auth.StreamingSessionData
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.ContentTypes
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions, EvidenceRecordBufferedEvent}
import services.rtm.{RtmClient, RtmRequest}
import utils.{HdlResponseHelpers, RequestUtils, WSResponseHelpers}

import scala.concurrent.ExecutionContext

class ProbeController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  sessionData: StreamingSessionData,
  audit: AuditClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with WSResponseHelpers
    with HdlResponseHelpers {

  def probe: Action[AnyContent] =
    (heimdallRequestAction andThen permValidation.build(PermissionType.View) andThen rtmRequestAction).async {
      request =>
        val authHandler = request.authorizationData
        val auditEvents: List[EvidenceRecordBufferedEvent] =
          request.media.toList.map(
            file =>
              EvidenceRecordBufferedEvent(
                evidenceTid(file.evidenceId, file.partnerId),
                updatedByTid(authHandler.parsedJwt),
                fileTid(file.fileId, file.partnerId),
                RequestUtils.getClientIpAddress(request)
            )
          )

        (
          for {
            response <- FutureEither(rtm.send(request).map(withOKStatus))
            _ <- FutureEither(audit.recordEndSuccess(auditEvents))
              .mapLeft(toHttpStatus("failedToSendMediaViewedAuditEvent")(_, Some(request.media)))
          } yield toProbeResult(response, request)
        ).fold(error, Ok(_).as(ContentTypes.JSON))
    }

  private def generateStreamingToken(request: RtmRequest[AnyContent]): String = {
    val authHandler           = request.attrs(AuthorizationAttr.Key)
    val streamingSessionToken = sessionData.createToken(authHandler.token, request.media.getSortedFileIds)
    streamingSessionToken
  }

  private def toProbeResult(response: WSResponse, request: RtmRequest[AnyContent]): JsObject = {
    val streamingToken = generateStreamingToken(request)

    val responseWithToken = response.json
      .as[JsObject] + ("streamingSessionToken" -> Json.toJson(streamingToken))

    responseWithToken
  }

}
