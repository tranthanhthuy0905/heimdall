package controllers

import actions.{HeimdallRequestAction, TokenValidationAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions, EvidenceFileStreamedEvent, AuditEvent}
import utils.{HdlResponseHelpers, RequestUtils, AuditEventHelpers}
import services.apidae.ApidaeClient
import scala.concurrent.ExecutionContext
import scala.concurrent.Future 
import services.sage.SageClient

class AuditController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  audit: AuditClient,
  apidae: ApidaeClient,
  sage: SageClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with HdlResponseHelpers
    with AuditEventHelpers {

  def recordMediaStreamedEvent: Action[AnyContent] =
    (heimdallRequestAction andThen tokenValidationAction).async { request =>
      (for {
        auditEvents <- FutureEither(Future.traverse(request.media.toList)( 
            file => {
              val streamedEvent = EvidenceFileStreamedEvent(
                evidenceTid(file.evidenceId, file.partnerId),
                updatedByTid(request.parsedJwt),
                fileTid(file.fileId, file.partnerId),
                RequestUtils.getClientIpAddress(request)
              )
              getZipInfoAndDecideEvent(sage, apidae, file, streamedEvent, buildZipFileStreamedEvent).future
            }
        ).map(toEitherOfList))
        auditResult <- FutureEither(audit.recordEndSuccess(auditEvents))
          .mapLeft(toHttpStatus("failedToSendMediaStreamedAuditEvent")(_, Some(request.media)))
       } yield auditResult).fold(error, _ => Ok(Json.obj("status" -> "ok")).as(ContentTypes.JSON))
    }
}
