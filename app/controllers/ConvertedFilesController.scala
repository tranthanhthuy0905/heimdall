package controllers

import actions.{ConvertedFilesRequestAction, FeatureValidationActionBuilder, HeimdallRequestAction, PermValidationActionBuilder, TokenValidationAction}
import com.evidence.service.common.logging.LazyLogging
import models.common.{AuthorizationAttr, PermissionType}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.sage.{ConvertedFile, SageClient}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConvertedFilesController @Inject()(heimdallRequestAction: HeimdallRequestAction,
                                         tokenValidationAction: TokenValidationAction,
                                         permValidation: PermValidationActionBuilder,
                                         featureValidationAction: FeatureValidationActionBuilder,
                                         convertedFilesRequestAction: ConvertedFilesRequestAction,
                                         sage: SageClient,
                                         components: ControllerComponents)(implicit ex: ExecutionContext)
  extends AbstractController(components)
    with LazyLogging {

  def getConvertedFiles: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen convertedFilesRequestAction
      ).async { implicit request =>

      def filterExtractionInfo(id: services.sage.EvidenceId, audienceId: String, files: Seq[ConvertedFile]): Seq[ConvertedFile] = {
        if (audienceId != id.partnerId.toString) files.map(file => file.copy(extractions = Seq.empty)) else files
      }

      val authHandler = request.attrs(AuthorizationAttr.Key)
      val evidenceId = services.sage.EvidenceId(request.file.evidenceId, request.file.partnerId)

      sage.
        getConvertedFiles(services.sage.EvidenceId(request.file.evidenceId, request.file.partnerId)).
        map {
          case Right(files) => Ok(ConvertedFile.convertedFilesToJsonValue(filterExtractionInfo(evidenceId, authHandler.parsedJwt.audienceId, files)))
          case Left(error) =>
            InternalServerError(error.message)
        }
    }
}
