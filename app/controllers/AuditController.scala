package controllers

import actions.{HeimdallRequestAction, TokenValidationAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions, AuditEvent, EvidenceFileStreamedEvent}
import utils.RequestUtils

import scala.concurrent.{ExecutionContext, Future}

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

      FutureEither(logAuditEvent(auditEvents))
        .fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), _ => Ok(Json.obj("status" -> "ok")))
    }

  private def logAuditEvent(event: List[AuditEvent]): Future[Either[Int, List[String]]] = {
    audit.recordEndSuccess(event).map(Right(_)).recover { case _ => Left(INTERNAL_SERVER_ERROR) }
  }
}
