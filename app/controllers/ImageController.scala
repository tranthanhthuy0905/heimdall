package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither

import javax.inject.Inject
import models.common.{AuditEventType, PermissionType}
import play.api.http.ContentTypes
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc._

import scala.concurrent.ExecutionContext
import services.audit.{AuditClient, AuditConversions}
import services.rti.metadata.MetadataJsonConversions
import services.rti.RtiClient
import utils.{AuditEventHelpers, HdlResponseHelpers, JsonFormat, WSResponseHelpers}
import services.apidae.ApidaeClient
import services.sage.SageClient

class ImageController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  auditEventAction: AuditEventActionBuilder,
  zipAuditEventAction: ZipAuditEventActionBuilder,
  watermarkAction: WatermarkAction,
  rtiRequestAction: RtiRequestAction,
  rti: RtiClient,
  audit: AuditClient,
  sage: SageClient,
  apidae: ApidaeClient,
  components: ControllerComponents)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with MetadataJsonConversions
    with JsonFormat
    with WSResponseHelpers
    with HdlResponseHelpers 
    with AuditEventHelpers {

  def view: Action[AnyContent] = {
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen (zipAuditEventAction.build(AuditEventType.ZipFileReviewed) compose auditEventAction.build(AuditEventType.EvidenceReviewed))
        andThen watermarkAction
        andThen rtiRequestAction
    ).async { implicit request =>
      (for {
        auditEvent <- FutureEither.successful(request.request.auditEvent.toRight(INTERNAL_SERVER_ERROR))
        response <- FutureEither(rti.transcode(request.presignedUrl, request.watermark, request.file).map(withOKStatus))
        _ <- FutureEither(audit.recordEndSuccess(auditEvent)).mapLeft(toHttpStatus("failedToSendEvidenceViewedAuditEvent")(_))
      } yield response).fold(error, streamedSuccessResponse(_, "image/jpeg"))
    }
  }

  def zoom: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen (zipAuditEventAction.build(AuditEventType.ZipFileReviewed) compose auditEventAction.build(AuditEventType.EvidenceReviewed))
        andThen watermarkAction
        andThen rtiRequestAction
    ).async { implicit request =>
      (for {
        auditEvent <- FutureEither.successful(request.request.auditEvent.toRight(INTERNAL_SERVER_ERROR))
        response <- FutureEither(rti.zoom(request.presignedUrl, request.watermark, request.file).map(withOKStatus))
        _ <- FutureEither(audit.recordEndSuccess(auditEvent))
          .mapLeft(toHttpStatus("failedToSendEvidenceViewedAuditEvent")(_))
      } yield response).fold(error, streamedSuccessResponse(_, "image/jpeg"))
    }

  def metadata: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen rtiRequestAction
    ).async { implicit request =>
      (for {
        response <- FutureEither(rti.metadata(request.presignedUrl, request.file).map(withOKStatus))
      } yield toMetadataEntity(response)).fold(error, Ok(_).as(ContentTypes.JSON))
    }

  private def toMetadataEntity(response: WSResponse): JsObject = {
    val metadata = metadataFromJson(response.json)
    removeNullValues(Json.toJson(metadata))
  }
}
