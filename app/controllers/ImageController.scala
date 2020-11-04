package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.HttpEntity
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import services.audit.{AuditClient, AuditConversions, EvidenceReviewEvent}
import services.rti.metadata.MetadataJsonConversions
import services.rti.RtiClient
import utils.JsonFormat

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
    with JsonFormat {

  def view: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen watermarkAction
        andThen rtiRequestAction
    ).async { implicit request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)

      for {
        response <- rti.transcode(request.presignedUrl, request.watermark, request.file)
        _ <- audit.recordEndSuccess(
          EvidenceReviewEvent(
            evidenceTid(request.file.evidenceId, request.file.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(request.file.fileId, request.file.partnerId),
            request.request.clientIpAddress
          ))
        result <- {
          Future.successful(toResult(response))
        }
      } yield result.fold(BadRequest(_), r => r)
    }

  def extractThumbnail: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen featureValidationAction.build("edc.thumbnail_extraction.enable")
        andThen rtiThumbnailRequestAction
    ).async { implicit request =>
      for {
        response <- rti.thumbnail(request.presignedUrl, request.width, request.height, request.file)
        result <- {
          Future.successful(toResult(response))
        }
      } yield result.fold(BadRequest(_), r => r)
    }

  def zoom: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen watermarkAction
        andThen rtiRequestAction
    ).async { implicit request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)

      for {
        response <- rti.zoom(request.presignedUrl, request.watermark, request.file)
        _ <- audit.recordEndSuccess(
          EvidenceReviewEvent(
            evidenceTid(request.file.evidenceId, request.file.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(request.file.fileId, request.file.partnerId),
            request.request.clientIpAddress
          ))
        result <- Future.successful(toResult(response))
      } yield result.fold(BadRequest(_), r => r)
    }

  def metadata: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen rtiRequestAction
    ).async { implicit request =>
      for {
        response <- rti.metadata(request.presignedUrl, request.file).filter(_.status equals OK)
        result   <- Future.successful(toMetadataEntity(response))
      } yield result.fold(BadRequest(_), Ok(_))

    }

  private def toResult(response: WSResponse): Either[JsObject, Result] = {
    Some(response.status)
      .filter(_ equals play.api.http.Status.OK)
      .toRight(Json.obj("message" -> response.body[String]))
      .map(_ => {
        val contentType = response.headers.getOrElse("Content-Type", Seq()).headOption.getOrElse("image/jpeg")

        response.headers
          .get("Content-Length")
          .map(length =>
            Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), Some(contentType))))
          .getOrElse(Ok.chunked(response.bodyAsSource).as(contentType))
      })
  }

  private def toMetadataEntity(response: WSResponse): Either[JsObject, JsValue] = {
    Some(response.status)
      .filter(_ equals play.api.http.Status.OK)
      .toRight(Json.obj("message" -> response.body[String]))
      .map(_ => {
        val metadata = metadataFromJson(response.json)
        removeNullValues(Json.toJson(metadata).as[JsObject])
      })
  }
}
