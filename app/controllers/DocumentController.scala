package controllers

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtiRequestAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.HttpEntity
import play.api.libs.ws.WSResponse
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import services.audit.{AuditClient, AuditConversions, AuditEvent, EvidenceReviewEvent}
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
      val viewEvent = EvidenceReviewEvent(
        evidenceTid(request.file.evidenceId, request.file.partnerId),
        updatedByTid(authHandler.parsedJwt),
        fileTid(request.file.fileId, request.file.partnerId),
        request.request.clientIpAddress
      )
      (
        for {
          response <- FutureEither(documentClient.view(request.presignedUrl).map(withOKStatus))
          _        <- FutureEither(logAuditEvent(viewEvent))
        } yield toResult(response)
      ).fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), r => r)
    }

  private def toResult(response: WSResponse): Result = {
    val contentType = response.headers.getOrElse("Content-Type", Seq()).headOption.getOrElse("application/pdf")
    response.headers
      .get("Content-Length")
      .map(length =>
        Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), Some(contentType))))
      .getOrElse(Ok.chunked(response.bodyAsSource).as(contentType))
  }

  private def logAuditEvent(event: AuditEvent): Future[Either[Int, String]] = {
    audit.recordEndSuccess(event).map(Right(_)).recover { case _ => Left(INTERNAL_SERVER_ERROR) }
  }

  private def withOKStatus(response: WSResponse): Either[Int, WSResponse] = {
    Some(response)
      .filter(_.status equals OK)
      .toRight(response.status)
  }
}
