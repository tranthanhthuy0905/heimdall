package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.ContentTypes
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc._

import scala.concurrent.ExecutionContext
import services.audit.{AuditClient, AuditConversions, EvidenceReviewEvent, ZipFileAccessedEvent, AuditEvent}
import services.rti.metadata.MetadataJsonConversions
import services.rti.RtiClient
import services.apidae.ApidaeClient
import utils.{HdlResponseHelpers, JsonFormat, WSResponseHelpers}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ImageController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  featureValidationAction: FeatureValidationActionBuilder,
  watermarkAction: WatermarkAction,
  rtiRequestAction: RtiRequestAction,
  rtiThumbnailRequestAction: ThumbnailRequestAction,
  rti: RtiClient,
  audit: AuditClient,
  apidae: ApidaeClient,
  components: ControllerComponents)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with MetadataJsonConversions
    with JsonFormat
    with WSResponseHelpers
    with HdlResponseHelpers {

  def view: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen watermarkAction
        andThen rtiRequestAction
    ).async { implicit request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)
      val zipFileResponseFuture = apidae.getZipFileInfo(request.file.partnerId, request.file.evidenceId, request.file.fileId)
      val zipFileResponse = Await.result(zipFileResponseFuture, Duration.Inf)

      var auditEvent : AuditEvent = null 
      val status = (zipFileResponse.json \ "status").as[String]
      // if this is a zip file then use zip audit message
      if (status == "success") {
        val evidenceTitle = (zipFileResponse.json \ "data" \ "file_name").as[String]
        val filePath = (zipFileResponse.json \ "data" \ "file_path").as[String]
        auditEvent = ZipFileAccessedEvent(
          evidenceTid(request.file.evidenceId, request.file.partnerId),
          updatedByTid(authHandler.parsedJwt),
          fileTid(request.file.fileId, request.file.partnerId),
          request.request.clientIpAddress,
          evidenceTitle,
          filePath
        )
      } 
      else {
        auditEvent = EvidenceReviewEvent(
          evidenceTid(request.file.evidenceId, request.file.partnerId),
          updatedByTid(authHandler.parsedJwt),
          fileTid(request.file.fileId, request.file.partnerId),
          request.request.clientIpAddress
        )
      }

      (for {
        response <- FutureEither(rti.transcode(request.presignedUrl, request.watermark, request.file).map(withOKStatus))
        _ <- FutureEither(audit.recordEndSuccess(auditEvent))
          .mapLeft(toHttpStatus("failedToSendEvidenceViewedAuditEvent")(_))
      } yield response).fold(error, streamedSuccessResponse(_, "image/jpeg"))
    }

  def extractThumbnail: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen featureValidationAction.build("edc.thumbnail_extraction.enable")
        andThen rtiThumbnailRequestAction
    ).async { implicit request =>
      FutureEither(
        rti
          .thumbnail(request.presignedUrl, request.width, request.height, request.file)
          .map(withOKStatus))
        .fold(error, streamedSuccessResponse(_, "image/jpeg"))
    }

  def zoom: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen watermarkAction
        andThen rtiRequestAction
    ).async { implicit request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)
      val viewAuditEvent = EvidenceReviewEvent(
        evidenceTid(request.file.evidenceId, request.file.partnerId),
        updatedByTid(authHandler.parsedJwt),
        fileTid(request.file.fileId, request.file.partnerId),
        request.request.clientIpAddress
      )

      (
        for {
          response <- FutureEither(rti.zoom(request.presignedUrl, request.watermark, request.file).map(withOKStatus))
          _ <- FutureEither(audit.recordEndSuccess(viewAuditEvent))
            .mapLeft(toHttpStatus("failedToSendEvidenceViewedAuditEvent")(_))
        } yield response
      ).fold(error, streamedSuccessResponse(_, "image/jpeg"))
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
