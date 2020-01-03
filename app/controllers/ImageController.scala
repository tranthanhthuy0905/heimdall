package controllers

import actions._
import akka.stream.scaladsl.Source
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.{HttpChunk, HttpEntity}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.audit.{AuditClient, AuditConversions, EvidenceReviewEvent}
import services.rti.metadata.MetadataJsonConversions
import services.rti.RtiClient
import utils.JsonFormat

class ImageController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  featureValidationAction: FeatureValidationActionBuilder,
  watermarkAction: WatermarkAction,
  rtiRequestAction: RtiRequestAction,
  rtiThumbnailRequestAction: ThumbnailRequestAction,
  rti: RtiClient,
  audit: AuditClient,
  config: Config,
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
        response <- rti.transcode(request.presignedUrl, request.watermark, request.file.fileId)
        _ <- audit.recordEndSuccess(
          EvidenceReviewEvent(
            evidenceTid(request.file.evidenceId, request.file.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(request.file.fileId, request.file.partnerId),
            request.request.clientIpAddress
          ))
        httpEntity <- {
          Future.successful(toHttpEntity(response))
        }
      } yield
        httpEntity.fold(BadRequest(_), entity => {
          Ok.sendEntity(entity)
        })
    }

  def extractThumbnail: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen featureValidationAction.build("edc.thumbnail_extraction.enable")
        andThen rtiThumbnailRequestAction
      ).async { implicit request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)
      for {
        response <- rti.thumbnail(request.presignedUrl, request.width, request.height)
        httpEntity <- {
          Future.successful(toHttpEntity(response))
        }
      } yield
        httpEntity.fold(BadRequest(_), entity => {
          Ok.sendEntity(entity)
        })
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
        response <- rti.zoom(request.presignedUrl, request.watermark, request.file.fileId)
        _ <- audit.recordEndSuccess(
          EvidenceReviewEvent(
            evidenceTid(request.file.evidenceId, request.file.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(request.file.fileId, request.file.partnerId),
            request.request.clientIpAddress
          ))
        httpEntity <- Future.successful(toHttpEntity(response))
      } yield
        httpEntity.fold(BadRequest(_), entity => {
          Ok.sendEntity(entity)
        })
    }

  def metadata: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen rtiRequestAction
    ).async { implicit request =>
      for {
        response   <- rti.metadata(request.presignedUrl)
        httpEntity <- Future.successful(toMetadataEntity(response))
      } yield
        httpEntity.fold(BadRequest(_), metadata => {
          Ok(metadata)
        })
    }

  private def toHttpEntity(response: WSResponse): Either[JsObject, HttpEntity] = {
    Some(response.status)
      .filter(_ equals play.api.http.Status.OK)
      .toRight(Json.obj("message" -> response.body))
      .map(_ => {
        val contentType = response.headers.getOrElse("Content-Type", Seq("image/jpeg")).headOption

        response.headers
          .get("Content-Length")
          .map(length =>
            HttpEntity
              .Streamed(response.bodyAsSource, Some(length.mkString.toLong), contentType))
          .getOrElse(HttpEntity.Chunked(Source.apply(List(HttpChunk.Chunk(response.bodyAsBytes))), contentType))
      })
  }

  private def toMetadataEntity(response: WSResponse): Either[JsObject, JsValue] = {
    Some(response.status)
      .filter(_ equals play.api.http.Status.OK)
      .toRight(Json.obj("message" -> response.body))
      .map(_ => {
        val metadata = metadataFromJson(response.json)
        removeNullValues(Json.toJson(metadata).as[JsObject])
      })
  }
}
