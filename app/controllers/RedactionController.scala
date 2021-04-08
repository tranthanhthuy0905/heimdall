package controllers

import actions.{HeimdallRequestAction, RedactionPermValidationAction, RedactionRequestActionBuilder}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import play.api.http.ContentTypes
import play.api.mvc._
import services.audit.AuditConversions
import services.drd.DrdClient
import utils.{HdlResponseHelpers, WSResponseHelpers}

import scala.concurrent.ExecutionContext

class RedactionController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  drdPermValidation: RedactionPermValidationAction,
  redactionRequestActionBuilder: RedactionRequestActionBuilder,
  drdClient: DrdClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with WSResponseHelpers
    with HdlResponseHelpers {

  def createDocumentRedaction(evidenceId: String): Action[AnyContent] =
    (
      heimdallRequestAction
        andThen redactionRequestActionBuilder.build(evidenceId)
        andThen drdPermValidation
    ).async { implicit request =>
      FutureEither(
        drdClient
          .createRedaction(request.partnerId, request.userId, request.evidenceId)
          .map(withOKStatus))
        .fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }
}
