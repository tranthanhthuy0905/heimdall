package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import play.api.mvc._
import models.common.PermissionType
import utils.{HdlResponseHelpers, WSResponseHelpers}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import services.metadata.MetadataClient

class MetadataController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  metadataRequestAction: MetadataRequestAction,
  metadataClient: MetadataClient,
  components: ControllerComponents)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with WSResponseHelpers
    with HdlResponseHelpers {

  def metadata: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen metadataRequestAction
    ).async { implicit request =>
      (for {
        response <- FutureEither(metadataClient.getMetadata(request.file, request.snapshotVersion).map(withOKStatus))
      } yield response).fold(error, streamedSuccessResponse(_, JSON))
    }
}
