package actions

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import scala.concurrent.{ExecutionContext, Future}
import services.sage.ConvertedFilesRequest
import utils.UUIDHelper

case class ConvertedFilesRequestAction @Inject()()(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[HeimdallRequest, ConvertedFilesRequest]
    with LazyLogging
    with UUIDHelper {

  def refine[A](request: HeimdallRequest[A]) = {

    val result = for {
      file     <- request.media.headOption.toRight(Results.BadRequest)
    } yield ConvertedFilesRequest(file, request)
    Future.successful(result)
  }
}
