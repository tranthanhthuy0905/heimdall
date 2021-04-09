package controllers

import actions.{HeimdallRequestAction, RedactionPermValidationAction, RedactionRequestActionBuilder}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
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
      // TODO: validate evidence type
      // TODO: Using sage/dredd to get Evidence Title from (evidenceId, partnerId)
      FutureEither(
        drdClient
          .call(
            s"/v1/evidences/${request.evidenceId}/redactions",
            request.method,
            request.partnerId,
            request.userId,
            Option(
              Json.obj(
                "EvidenceTitle" -> "a test title",
              ))
          )
          .map(withOKStatus))
        .fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }

  def callDocumentRedactionAPI(evidenceId: String, path: String): Action[AnyContent] =
    (
      heimdallRequestAction
        andThen redactionRequestActionBuilder.build(evidenceId)
        andThen drdPermValidation
    ).async { implicit request =>
      FutureEither(
        drdClient
          .call(
            s"/v1/evidences/${request.evidenceId}/${path}",
            request.method,
            request.partnerId,
            request.userId,
            request.body.asJson
          )
          .map(withOKStatus))
        .fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }
}
