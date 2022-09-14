package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither

import javax.inject.Inject
import models.common.{AuditEventType, AuthorizationAttr, PermissionType}
import play.api.http.ContentTypes
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext
import services.apidae.ApidaeClient
import services.audit.{AuditClient, AuditConversions, EvidencePlaybackRequested}
import services.rti.metadata.MetadataJsonConversions
import utils.{HdlResponseHelpers, WSResponseHelpers}

class MediaConvertController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  apidaeRequestAction: ApidaeRequestAction,
  auditEventAction: AuditEventActionBuilder,
  mediaConvertValidation: MediaConvertValidation,
  apidae: ApidaeClient,
  audit: AuditClient,
  components: ControllerComponents)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with MetadataJsonConversions
    with WSResponseHelpers
    with HdlResponseHelpers {

  def convert: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen auditEventAction.build(AuditEventType.EvidenceConversionRequested)
        andThen apidaeRequestAction
        andThen mediaConvertValidation
    ).async { implicit request =>

      (for {
        auditEvent <- FutureEither.successful(request.request.auditEvent.toRight(INTERNAL_SERVER_ERROR))
        _ <- FutureEither(audit.recordEndSuccess(auditEvent)).mapLeft(toHttpStatus("failedToSendEvidencePlaybackRequestedAuditEvent")(_))
        response <- FutureEither(
          apidae
            .transcode(request.file.partnerId, request.userId, request.file.evidenceId, request.file.fileId)
            .map(withOKStatus))
      } yield response).fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }

  def status: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
    ).async { implicit request =>
      FutureEither(
        apidae
          .getTranscodingStatus(request.file.partnerId, request.userId, request.file.evidenceId, request.file.fileId)
          .map(withOKStatus))
        .fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }
}
