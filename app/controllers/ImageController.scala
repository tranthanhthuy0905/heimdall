package controllers

import actions._
import akka.stream.scaladsl.Source
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.{HttpChunk, HttpEntity}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import services.audit.{AuditClient, AuditConversions, EvidenceViewed}
import services.rti.RtiClient

class ImageController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  watermarkAction: WatermarkAction,
  rtiRequestAction: RtiRequestAction,
  rti: RtiClient,
  audit: AuditClient,
  components: ControllerComponents)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions {

  def view: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen watermarkAction
        andThen rtiRequestAction
    ).async { implicit request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)

      for {
        response <- rti.getImage(request.presignedUrl.toString, request.watermark)
        _ <- audit.recordEndSuccess(
          EvidenceViewed(
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

  private def toHttpEntity(response: WSResponse): Either[JsObject, HttpEntity] = {
    Some(response.status)
      .filter(_ equals play.api.http.Status.OK)
      .toRight(Json.obj("message" -> response.body))
      .map(_ => {
        val contentType = response.headers.getOrElse("Content-Type", Seq("image/jpeg")).headOption

        response.headers
          .get("Content-Length")
          .map(length => HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), contentType))
          .getOrElse(HttpEntity.Chunked(Source.apply(List(HttpChunk.Chunk(response.bodyAsBytes))), contentType))
      })
  }
}
