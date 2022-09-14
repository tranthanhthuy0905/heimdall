package controllers

import actions.{AuditEventActionBuilder, HeimdallRequestAction, PermValidationActionBuilder, RtiRequestAction, ZipAuditEventActionBuilder}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither

import javax.inject.Inject
import models.common.{AuditEventType, PermissionType}
import play.api.mvc._

import scala.concurrent.ExecutionContext
import services.audit.{AuditClient, AuditConversions}
import services.document.DocumentClient
import utils.{AuditEventHelpers, HdlResponseHelpers, WSResponseHelpers}
import services.apidae.ApidaeClient
import services.sage.SageClient

class DocumentController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  rtiRequestAction: RtiRequestAction,
  auditEventAction: AuditEventActionBuilder,
  zipAuditEventAction: ZipAuditEventActionBuilder,
  audit: AuditClient,
  documentClient: DocumentClient,
  apidae: ApidaeClient,
  sage: SageClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with WSResponseHelpers
    with HdlResponseHelpers
    with AuditEventHelpers {

  def view: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen (zipAuditEventAction.build(AuditEventType.ZipFileReviewed) compose auditEventAction.build(AuditEventType.EvidenceReviewed))
        andThen rtiRequestAction
    ).async { implicit request =>
      (
        for {
          auditEvent <- FutureEither.successful(request.request.auditEvent.toRight(INTERNAL_SERVER_ERROR))
          response <- FutureEither(documentClient.view(request.presignedUrl).map(withOKStatus))
          _ <- FutureEither(audit.recordEndSuccess(auditEvent)).mapLeft(toHttpStatus("failedToSendEvidenceViewedAuditEvent")(_))
        } yield response
      ).fold(error, streamedSuccessResponse(_, "application/pdf"))
    }
}
