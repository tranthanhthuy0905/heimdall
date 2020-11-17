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
import services.audit.{AuditClient, AuditConversions, EvidenceReviewEvent}
import services.rti.metadata.MetadataJsonConversions
import services.rti.RtiClient
import utils.{HdlResponseHelpers, JsonFormat, WSResponseHelpers}

class ImageController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  featureValidationAction: FeatureValidationActionBuilder,
  watermarkAction: WatermarkAction,
  rtiRequestAction: RtiRequestAction,
  rtiThumbnailRequestAction: ThumbnailRequestAction,
  rti: RtiClient,
  audit: AuditClient,
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
      val viewAuditEvent = EvidenceReviewEvent(
        evidenceTid(request.file.evidenceId, request.file.partnerId),
        updatedByTid(authHandler.parsedJwt),
        fileTid(request.file.fileId, request.file.partnerId),
        request.request.clientIpAddress
      )

      (for {
        response <- FutureEither(rti.transcode(request.presignedUrl, request.watermark, request.file).map(withOKStatus))
        _ <- FutureEither(audit.recordEndSuccess(viewAuditEvent))
          .mapLeft(toHttpStatus("failedToSendEvidenceViewedAuditEvent")(_))
      } yield response).fold(error, streamed(_, "image/jpeg"))
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
        .fold(error, streamed(_, "image/jpeg"))
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
      ).fold(error, streamed(_, "image/jpeg"))
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
