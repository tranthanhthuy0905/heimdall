package controllers

import com.axon.sage.protos.v1.evidence_video_service.ConvertedFile
import actions.{ApidaeRequestAction, HeimdallRequestAction, PermValidationActionBuilder, TokenValidationAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.ContentTypes
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.sage.{SageClient, SageJson}
import utils.{HdlResponseHelpers, WSResponseHelpers}
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try
import com.typesafe.config.Config
import services.apidae.ApidaeClient

class ConvertedFilesController @Inject()(heimdallRequestAction: HeimdallRequestAction,
                                         tokenValidationAction: TokenValidationAction,
                                         permValidation: PermValidationActionBuilder,
                                         apidaeRequestAction: ApidaeRequestAction,
                                         sage: SageClient,
                                         apidae: ApidaeClient,
                                         config: Config,
                                         components: ControllerComponents)(implicit ex: ExecutionContext)
  extends AbstractController(components)
    with SageJson
    with LazyLogging
    with HdlResponseHelpers
    with WSResponseHelpers  {
  private val enable_recorded_on_overlay: Boolean = Try(config.getBoolean("edc.features.recorded_on_verlay")).getOrElse(true)

  def getConvertedFiles: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
      ).async { implicit request =>
      def filterExtractionInfo(id: services.sage.EvidenceId, audienceId: String, files: Seq[ConvertedFile]): Seq[ConvertedFile] = {
        if (audienceId != id.partnerId.toString) files.map(file => file.copy(extractions = Seq.empty)) else files
      }

      val authHandler = request.attrs(AuthorizationAttr.Key)
      val evidenceId = services.sage.EvidenceId(request.file.evidenceId, request.file.partnerId)

      if (enable_recorded_on_overlay) {
        FutureEither(
          apidae.getConvertedFiles(request.file.partnerId, request.file.evidenceId)
            .map(withOKStatus)
          ).fold(error, response => Ok(response.json).as(ContentTypes.JSON))
      }
      else {
        FutureEither(
          sage.
            getConvertedFiles(services.sage.EvidenceId(request.file.evidenceId, request.file.partnerId))).
            mapLeft(toHttpStatus("failedToSendEvidenceConvertedFilesEvent")(_))
          .fold(error, files => Ok(convertedFilesToJsonValue(filterExtractionInfo(evidenceId, authHandler.parsedJwt.audienceId, files))).as(ContentTypes.JSON))
      }
    }
}
