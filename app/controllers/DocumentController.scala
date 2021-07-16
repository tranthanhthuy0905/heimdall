package controllers

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtiRequestAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.mvc._

import scala.concurrent.ExecutionContext
import services.audit.{AuditClient, AuditConversions, EvidenceReviewEvent}
import services.document.DocumentClient
import utils.{HdlResponseHelpers, WSResponseHelpers}

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
    with AuditConversions
    with WSResponseHelpers
    with HdlResponseHelpers {

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
          _ <- FutureEither(audit.recordEndSuccess(viewEvent))
            .mapLeft(toHttpStatus("failedToSendEvidenceViewedAuditEvent")(_))
        } yield response
      ).fold(error, streamedSuccessResponse(_, "application/pdf"))
    }
}
