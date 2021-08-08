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

import scala.concurrent.Future
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
      apidae.getZipFileInfo(request.file.partnerId, request.file.evidenceId, request.file.fileId).map(
        response => {
          val status = (response.json \ "status").asOpt[String].getOrElse("")
          // if this is a zip file then use zip audit event
          if (status == "success") {
            val evidenceTitle = (response.json \ "data" \ "file_name").asOpt[String].getOrElse("")
            val filePath = (response.json \ "data" \ "file_path").asOpt[String].getOrElse("")
            ZipFileAccessedEvent(
              evidenceTid(request.file.evidenceId, request.file.partnerId),
              updatedByTid(authHandler.parsedJwt),
              fileTid(request.file.fileId, request.file.partnerId),
              request.request.clientIpAddress,
              evidenceTitle,
              filePath
            )
          }
          else {
            EvidenceReviewEvent(
              evidenceTid(request.file.evidenceId, request.file.partnerId),
              updatedByTid(authHandler.parsedJwt),
              fileTid(request.file.fileId, request.file.partnerId),
              request.request.clientIpAddress
            )
          }  
        }
      ).flatMap(auditEvent => {
        (for {
          response <- FutureEither(rti.transcode(request.presignedUrl, request.watermark, request.file).map(withOKStatus))
          _ <- FutureEither(audit.recordEndSuccess(auditEvent))
            .mapLeft(toHttpStatus("failedToSendEvidenceViewedAuditEvent")(_))
        } yield response).fold(error, streamedSuccessResponse(_, "image/jpeg"))
      }).recoverWith {
        case e: Exception => {
          logger.error(e, "Unexpected exception")(
            "path"       -> request.path,
            "method"     -> request.method,
            "evidenceId" -> request.file.evidenceId,
            "userId"     -> authHandler.parsedJwt.audienceId,
            "partnerId"  -> request.file.partnerId,
          )
          Future(error(INTERNAL_SERVER_ERROR))
        }
      }
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
