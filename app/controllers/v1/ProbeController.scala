package controllers.v1

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.auth.StreamingSessionData
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.HttpEntity
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions, EvidenceRecordBufferedEvent}
import services.rtm.{RtmClient, RtmRequest}
import utils.RequestUtils

import scala.concurrent.{ExecutionContext, Future}

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
    with AuditConversions {

  private def logAuditEvent(request: RtmRequest[AnyContent]): Future[Either[Int, List[String]]] = {
    val authHandler = request.attrs(AuthorizationAttr.Key)
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
    audit.recordEndSuccess(auditEvents).map(Right(_)).recover {
      case exception =>
        logger.error(
          exception,
          "failedToSendMediaViewedAuditEvent"
        )(
          "exception"   -> exception.getMessage,
          "path"        -> request.path,
          "mediaIndent" -> request.media,
          "token"       -> request.streamingSessionToken
        )
        Left(INTERNAL_SERVER_ERROR)
    }
  }

  private def generateStreamingToken(request: RtmRequest[AnyContent]): String = {
    val authHandler           = request.attrs(AuthorizationAttr.Key)
    val streamingSessionToken = sessionData.createToken(authHandler.token, request.media.getSortedFileIds)
    streamingSessionToken
  }

  private def setContentType(response: WSResponse): String = {
    response.headers
      .get("Content-Type")
      .flatMap(_.headOption)
      .getOrElse("application/json")
  }

  private def toResult(response: WSResponse, request: RtmRequest[AnyContent]): Result = {
    val streamingToken = generateStreamingToken(request)
    val contentType    = setContentType(response)

    val responseWithToken = response.json
      .as[JsObject] + ("streamingSessionToken" -> Json.toJson(streamingToken))

    Ok(responseWithToken).as(contentType)
  }

  private def withOKStatus(response: WSResponse): Either[Int, WSResponse] = {
    Some(response)
      .filter(_.status equals OK)
      .toRight(response.status)
  }

  def probe: Action[AnyContent] =
    (heimdallRequestAction andThen permValidation.build(PermissionType.View) andThen rtmRequestAction).async {
      request =>
        (
          for {
            response <- FutureEither(rtm.send(request).map(withOKStatus))
            _        <- FutureEither(logAuditEvent(request))
          } yield toResult(response, request)
        ).fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), r => r)

    }
}
