package controllers

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtiRequestAction}
import akka.stream.scaladsl.Source
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.{HttpChunk, HttpEntity}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.audit.{AuditClient, AuditConversions, EvidenceReviewEvent}
import services.document.DocumentClient

class DocumentController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  rtiRequestAction: RtiRequestAction,
  audit: AuditClient,
  documentClient: DocumentClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions {

  def view: Action[AnyContent] =
    (
      heimdallRequestAction andThen permValidation.build(PermissionType.View) andThen rtiRequestAction
    ).async { implicit request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)

      for {
        response <- documentClient.view(request.presignedUrl)
        _ <- audit.recordEndSuccess(
          EvidenceReviewEvent(
            evidenceTid(request.file.evidenceId, request.file.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(request.file.fileId, request.file.partnerId),
            request.request.clientIpAddress
          ))
        httpEntity <- Future.successful(toHttpEntity(response))
      } yield httpEntity.fold(BadRequest(_), entity => Ok.sendEntity(entity))
    }

  private def toHttpEntity(response: WSResponse): Either[JsObject, HttpEntity] = {
    Some(response.status)
      .filter(_ equals play.api.http.Status.OK)
      .toRight(Json.obj("message" -> response.body))
      .map(_ => {
        val contentType = "application/pdf"

        response.headers
          .get("Content-Length")
          .map(length => HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), Some(contentType)))
          .getOrElse(HttpEntity.Chunked(Source.apply(List(HttpChunk.Chunk(response.bodyAsBytes))), Some(contentType)))
      })
  }

}
