package controllers

import com.axon.sage.protos.v1.evidence_video_service.ConvertedFile
import actions.{ApidaeRequestAction, HeimdallRequestAction, PermValidationActionBuilder, TokenValidationAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.ContentTypes
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.sage.{SageClient, SageJson}
import utils.HdlResponseHelpers
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConvertedFilesController @Inject()(heimdallRequestAction: HeimdallRequestAction,
                                         tokenValidationAction: TokenValidationAction,
                                         permValidation: PermValidationActionBuilder,
                                         apidaeRequestAction: ApidaeRequestAction,
                                         sage: SageClient,
                                         components: ControllerComponents)(implicit ex: ExecutionContext)
  extends AbstractController(components)
    with SageJson
    with LazyLogging
    with HdlResponseHelpers  {

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

      (for {
        result <- FutureEither(
          sage.
            getConvertedFiles(services.sage.EvidenceId(request.file.evidenceId, request.file.partnerId))
        ).mapLeft(toHttpStatus("failedToSendEvidenceConvertedFilesEvent")(_))
      } yield result).fold(error, files => Ok(convertedFilesToJsonValue(filterExtractionInfo(evidenceId, authHandler.parsedJwt.audienceId, files))).as(ContentTypes.JSON))
    }
}
