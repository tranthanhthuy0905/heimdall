package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import models.common.{AuditEventType, AuthorizationAttr, PermissionType}
import play.api.http.ContentTypes
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.apidae.ApidaeClient
import services.audit.{AuditClient, AuditConversions, EvidenceLoadedForReviewEvent}
import utils.{HdlResponseHelpers, WSResponseHelpers}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
class ZipController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  apidaeRequestAction: ApidaeRequestAction,
  zipAuditEventAction: ZipAuditEventActionBuilder,
  apidae: ApidaeClient,
  audit: AuditClient,
  components: ControllerComponents)(implicit ex: ExecutionContext)
      extends AbstractController(components)
      with LazyLogging
      with AuditConversions
      with WSResponseHelpers
      with HdlResponseHelpers  {

  def getStatus: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
    ).async { implicit request =>
      FutureEither(
        apidae.
          getZipStatus(request.file.partnerId, request.file.evidenceId, request.file.fileId).
          map(withOKStatus))
        .fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }

  def getStructure: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen zipAuditEventAction.build(AuditEventType.ZipEvidenceLoaded)
        andThen apidaeRequestAction
    ).async { implicit request =>
      (for {
        auditEvent <- FutureEither.successful(request.request.auditEvent.toRight(INTERNAL_SERVER_ERROR))
        response <- FutureEither(apidae.getZipStructure(request.file.partnerId, request.file.evidenceId, request.file.fileId).map(withOKStatus))
        _ <- FutureEither(audit.recordEndSuccess(auditEvent))
          .mapLeft(toHttpStatus("failedToSendEvidenceLoadedForReviewEvent")(_))
        } yield response).fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }
}
