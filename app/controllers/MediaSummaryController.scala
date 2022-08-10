package controllers

import actions.{ApidaeRequestAction, FeatureValidationActionBuilder, HeimdallRequestAction, PermValidationActionBuilder}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import models.common.PermissionType
import play.api.http.ContentTypes
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.apidae.ApidaeClient
import utils.{HdlResponseHelpers, WSResponseHelpers}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class MediaSummaryController @Inject()(heimdallRequestAction: HeimdallRequestAction,
                               permValidation: PermValidationActionBuilder,
                               featureValidationAction: FeatureValidationActionBuilder,
                               apidaeRequestAction: ApidaeRequestAction,
                               apidae: ApidaeClient,
                               components: ControllerComponents)(implicit ex: ExecutionContext)
  extends AbstractController(components)
    with LazyLogging
    with WSResponseHelpers
    with HdlResponseHelpers  {

  def getMediaSummary: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen featureValidationAction.build("edc.service.apidae.enable")
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
      ).async { implicit request =>
      FutureEither(
        apidae.
          getMediaSummary(request.file.partnerId, request.file.evidenceId, request.file.fileId).
          map(withOKStatus))
        .fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }
}