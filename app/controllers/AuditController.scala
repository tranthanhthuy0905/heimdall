package controllers

import actions.{HeimdallRequestAction, TokenValidationAction}
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions, EvidenceFileStreamedEvent}
import utils.RequestUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AuditController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  audit: AuditClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions {

  def recordMediaStreamedEvent: Action[AnyContent] =
    (heimdallRequestAction andThen tokenValidationAction).async { request =>
      val auditEvents: List[EvidenceFileStreamedEvent] =
        request.media.toList.map(
          file =>
            EvidenceFileStreamedEvent(
              evidenceTid(file.evidenceId, file.partnerId),
              updatedByTid(request.parsedJwt),
              fileTid(file.fileId, file.partnerId),
              RequestUtils.getClientIpAddress(request)
          )
        )

      audit.recordEndSuccess(auditEvents).map(_ => Ok(Json.obj("status" -> "ok"))).recoverWith {
        case exception =>
          logger.error(
            exception,
            "failedToSendMediaStreamedAuditEvent"
          )(
            "exception"  -> exception.getMessage,
            "path"       -> request.path,
            "mediaIdent" -> request.media,
            "token"      -> request.streamingSessionToken
          )
          Future.successful(InternalServerError)
      }
    }
}
