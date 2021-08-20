package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import play.api.libs.json.Json
import play.api.mvc._
import services.apidae.{ApidaeClient}
import utils.{HdlResponseHelpers, WSResponseHelpers}
import services.audit.{AuditClient, AuditConversions, VideoConcatenationRequestedEvent}
import models.common.AuthorizationAttr

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConcatenationController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  concatenationAction: ConcatenationRequestAction,
  concatenationPermValidation: ConcatenationPermValidationAction,
  mediaValidation: ConcatenationValidation,
  audit: AuditClient,
  apidae: ApidaeClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with WSResponseHelpers
    with HdlResponseHelpers {

  def requestConcat: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen concatenationAction
        andThen mediaValidation
        andThen concatenationPermValidation
    ).async { request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)
      val requesterTid = updatedByTid(authHandler.parsedJwt)
      val auditEvent = VideoConcatenationRequestedEvent(
          requesterTid, requesterTid, requesterTid,
          request.request.clientIpAddress,
          request.title
      )
      (for {
        response <- FutureEither(apidae.requestConcatenate(Json.toJson(request)).map(withOKStatus))
        _ <- FutureEither(audit.recordEndSuccess(auditEvent)).mapLeft(toHttpStatus("failedToSendVideoConcatenationRequestedEvent")(_))
      } yield response).fold(error, streamedSuccessResponse(_, JSON))
    }
}
