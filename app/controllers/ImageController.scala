package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.HttpEntity
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import services.audit.{AuditClient, AuditConversions, AuditEvent, EvidenceReviewEvent}
import services.rti.metadata.MetadataJsonConversions
import services.rti.RtiClient
import utils.{JsonFormat, WSResponseHelpers}

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
    with WSResponseHelpers {

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
        _        <- FutureEither(logAuditEvent(viewAuditEvent))
      } yield toResult(response)).fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), r => r)
    }

  def extractThumbnail: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen featureValidationAction.build("edc.thumbnail_extraction.enable")
        andThen rtiThumbnailRequestAction
    ).async { implicit request =>
      (for {
        response <- FutureEither(
          rti.thumbnail(request.presignedUrl, request.width, request.height, request.file).map(withOKStatus))
      } yield toResult(response)).fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), r => r)
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
          _        <- FutureEither(logAuditEvent(viewAuditEvent))
        } yield toResult(response)
      ).fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), r => r)
    }

  def metadata: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen rtiRequestAction
    ).async { implicit request =>
      (for {
        response <- FutureEither(rti.metadata(request.presignedUrl, request.file).map(withOKStatus))
      } yield toMetadataEntity(response)).fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), r => r)
    }

  private def logAuditEvent(event: AuditEvent): Future[Either[Int, String]] = {
    audit.recordEndSuccess(event).map(Right(_)).recover { case _ => Left(INTERNAL_SERVER_ERROR) }
  }

  private def toResult(response: WSResponse): Result = {
    val contentType = response.headers.getOrElse("Content-Type", Seq()).headOption.getOrElse("image/jpeg")
    response.headers
      .get("Content-Length")
      .map(length =>
        Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), Some(contentType))))
      .getOrElse(Ok.chunked(response.bodyAsSource).as(contentType))
  }

  private def toMetadataEntity(response: WSResponse): Result = {
    val metadata = metadataFromJson(response.json)
    Ok(removeNullValues(Json.toJson(metadata).as[JsObject]))
  }
}
