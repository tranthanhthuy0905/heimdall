package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import play.api.libs.json.Json
import play.api.mvc._
import services.apidae.{ApidaeClient}
import utils.{HdlResponseHelpers, WSResponseHelpers}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConcatenationController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  concatenationAction: ConcatenationRequestAction,
  concatenationPermValidation: ConcatenationPermValidationAction,
  mediaValidation: ConcatenationValidation,
  apidae: ApidaeClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with WSResponseHelpers
    with HdlResponseHelpers {

  def requestConcat: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen concatenationAction
        andThen mediaValidation
        andThen concatenationPermValidation
    ).async { request =>
    println(Json.toJson(request))
    println(request)
      (for {
        response <- FutureEither(apidae.requestConcatenate(Json.toJson(request)).map(withOKStatus))
      } yield response).fold(error, streamedSuccessResponse(_, JSON))
    }
}
