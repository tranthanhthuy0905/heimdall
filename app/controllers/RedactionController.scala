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
    callDocumentRedactionAPI(evidenceId)

  def getDocumentRedactions(evidenceId: String): Action[AnyContent] = callDocumentRedactionAPI(evidenceId)

  def deleteDocumentRedaction(evidenceId: String, redactionId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId)

  def getXfdf(evidenceId: String, redactionId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId)

  def postXfdfCommands(evidenceId: String, redactionId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId)

  def createExtraction(evidenceId: String, redactionId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId)

  private def callDocumentRedactionAPI(evidenceId: String): Action[AnyContent] =
    (
      heimdallRequestAction
        andThen redactionRequestActionBuilder.build(evidenceId)
        andThen drdPermValidation
    ).async { implicit request =>
      FutureEither(
        drdClient
          .call(
            request.path,
            request.method,
            request.partnerId,
            request.userId,
            request.body.asJson
          )
          .map(withOKStatus))
        .fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }
}
