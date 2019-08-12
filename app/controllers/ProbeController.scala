package controllers

import actions.{
  HeimdallRequestAction,
  PermValidationActionBuilder,
  RtmRequestAction,
  TokenValidationAction,
  WatermarkAction
}
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.auth.StreamingSessionData
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Results}
import services.audit.{AuditClient, AuditConversions, EvidenceRecordBufferedEvent}
import services.rtm.{RtmClient, RtmResponseHandler}
import utils.RequestUtils
import com.typesafe.config.Config
import models.common.{AuthorizationAttr, PermissionType}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ProbeController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  watermarkAction: WatermarkAction,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  sessionData: StreamingSessionData,
  audit: AuditClient,
  config: Config,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions {

  def probe: Action[AnyContent] =
    (heimdallRequestAction andThen permValidation.build(PermissionType.View) andThen rtmRequestAction).async {
      request =>
        val authHandler = request.attrs(AuthorizationAttr.Key)

        val okCallback = (response: WSResponse) => {
          val streamingSessionToken = sessionData.createToken(authHandler.token, request.media.getSortedFileIds)

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

          val contentType = response.headers
            .get("Content-Type")
            .flatMap(_.headOption)
            .getOrElse("application/json")

          Try(audit.recordEndSuccess(auditEvents)) match {
            case Success(value) =>
              value.map { _ =>
                val responseWithToken = response.json
                  .as[JsObject] + ("streamingSessionToken" -> Json
                  .toJson(streamingSessionToken))
                Ok(responseWithToken).as(contentType)
              } recoverWith {
                case exception: Exception =>
                  logger.error(
                    exception,
                    "failedToSendMediaViewedAuditEvent"
                  )(
                    "exception"  -> exception.getMessage,
                    "path"       -> request.path,
                    "mediaIdent" -> request.media,
                    "token"      -> request.streamingSessionToken
                  )
                  Future.successful(InternalServerError)
              }
            case Failure(exception) =>
              logger.error(exception, "exceptionDuringEngagingAuditClient")(
                "exception"  -> exception.getMessage,
                "path"       -> request.path,
                "mediaIdent" -> request.media,
                "token"      -> request.streamingSessionToken
              )
              Future.successful(InternalServerError)
          }
        }

        rtm.send(request.toString) flatMap { response =>
          RtmResponseHandler(
            response,
            okCallback,
            Seq[(String, Any)](
              "path"    -> request.path,
              "media"   -> request.media,
              "status"  -> response.status,
              "message" -> response.body
            )
          )
        }
    }
}
