package controllers

import actions.{HeimdallRequestAction, RedactionPermValidationAction, RedactionRequestActionBuilder}
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import play.api.http.ContentTypes
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.drd.DrdClient
import utils.{HdlResponseHelpers, WSResponseHelpers}

import scala.concurrent.{ExecutionContext, Future}

class RedactionController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  drdPermValidation: RedactionPermValidationAction,
  redactionRequestActionBuilder: RedactionRequestActionBuilder,
  drdClient: DrdClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with WSResponseHelpers
    with HdlResponseHelpers {

  def createDocumentRedaction(evidenceId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId)

  def getDocumentRedactions(evidenceId: String): Action[AnyContent] = callDocumentRedactionAPI(evidenceId)

  def getRedactionData(evidenceId: String, redactionId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId)

  def deleteDocumentRedaction(evidenceId: String, redactionId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId)

  def getXfdf(evidenceId: String, redactionId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId)

  def postXfdfCommands(evidenceId: String, redactionId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId)

  def createExtraction(evidenceId: String, redactionId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId)

  def createOCRTask(evidenceId: String, evidencePartnerId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId, Option(evidencePartnerId))

  def getOCRTaskStatus(evidenceId: String, evidencePartnerId: String): Action[AnyContent] =
    callDocumentRedactionAPI(evidenceId, Option(evidencePartnerId))

  private def callDocumentRedactionAPI(
    evidenceId: String,
    evidencePartnerId: Option[String] = None): Action[AnyContent] =
    (
      heimdallRequestAction
        andThen redactionRequestActionBuilder.build(evidenceId, evidencePartnerId)
        andThen drdPermValidation
    ).async { implicit request =>
      drdClient
        .call(
          request.uri,
          request.method,
          request.userId,
          request.userPartnerId,
          request.body.asJson,
          request.remoteAddress
        )
        .map(streamedResponse(_, ContentTypes.JSON))
        .recoverWith {
          case e: Exception =>
            logger.error(e, "Unexpected Exception in callDocumentRedactionAPI")(
              "path"              -> request.path,
              "method"            -> request.method,
              "evidenceId"        -> request.evidenceId,
              "evidencePartnerId" -> request.evidencePartnerId,
              "userId"            -> request.userId,
              "userPartnerId"     -> request.userPartnerId,
            )
            Future.successful(InternalServerError)
        }
    }
}
