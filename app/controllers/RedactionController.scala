package controllers

import actions.{ApidaeRequestAction, DrdEvidenceRequestAction, FeatureValidationActionBuilder, HeimdallRequestAction, PermValidationActionBuilder, RtiRequestAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.ContentTypes
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions, EvidenceReviewEvent}
import services.document.DocumentClient
import services.drd.DrdClient
import utils.{HdlResponseHelpers, WSResponseHelpers}

import scala.concurrent.ExecutionContext

class RedactionController @Inject()(
                                     heimdallRequestAction: HeimdallRequestAction,
                                     permValidation: PermValidationActionBuilder,
                                     drdRequestAction: DrdEvidenceRequestAction,
                                     drdClient: DrdClient,
                                     components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with WSResponseHelpers
    with HdlResponseHelpers {

  def createDocumentRedaction: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View) andThen drdRequestAction
        // TODO: verify permission on the evidence
    ).async { implicit request =>
      FutureEither(
        drdClient
          .createRedaction(request.partnerId, request.userId, request.evidenceId)
          .map(withOKStatus))
        .fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }
}
