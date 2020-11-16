package controllers

import actions.{HeimdallRequestAction, TokenValidationAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions, EvidenceFileStreamedEvent}
import utils.{HdlResponseHelpers, RequestUtils}

import scala.concurrent.ExecutionContext

class AuditController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  audit: AuditClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with HdlResponseHelpers {

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

      FutureEither(audit.recordEndSuccess(auditEvents))
        .mapLeft(toHttpStatus("failedToSendMediaStreamedAuditEvent")(_, Some(request.media)))
        .fold(error, _ => Ok(Json.obj("status" -> "ok")).as(ContentTypes.JSON))
    }
}
