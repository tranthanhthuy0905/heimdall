package controllers

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtiRequestAction}
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.HttpEntity
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import scala.concurrent.ExecutionContext
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
        response <- documentClient.view(request.presignedUrl).filter(_.status equals OK)
        _ <- audit.recordEndSuccess(
          EvidenceReviewEvent(
            evidenceTid(request.file.evidenceId, request.file.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(request.file.fileId, request.file.partnerId),
            request.request.clientIpAddress
          ))
      } yield {
        val contentType = "application/pdf"
        response.headers
          .get("Content-Length")
          .map(length =>
            Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), Some(contentType))))
          .getOrElse(Ok.chunked(response.bodyAsSource).as(contentType))
      }
    }
}
