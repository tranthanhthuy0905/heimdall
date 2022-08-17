package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import models.common.{AuthorizationAttr, PermissionType}
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
  featureValidationAction: FeatureValidationActionBuilder,
  apidaeRequestAction: ApidaeRequestAction,
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
        andThen featureValidationAction.build("edc.service.apidae.enable")
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
        andThen featureValidationAction.build("edc.service.apidae.enable")
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
    ).async { implicit request =>

      val authHandler = request.attrs(AuthorizationAttr.Key)
      val auditEvent = EvidenceLoadedForReviewEvent(
        evidenceTid(request.file.evidenceId, request.file.partnerId),
        updatedByTid(authHandler.parsedJwt),
        fileTid(request.file.fileId, request.file.partnerId),
        request.request.clientIpAddress
      )
      (for {
        response <- FutureEither(apidae.getZipStructure(request.file.partnerId, request.file.evidenceId, request.file.fileId).map(withOKStatus))
        _ <- FutureEither(audit.recordEndSuccess(auditEvent))
          .mapLeft(toHttpStatus("failedToSendEvidenceLoadedForReviewEvent")(_))
        } yield response).fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }
}
