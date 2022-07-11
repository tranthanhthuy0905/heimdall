package actions

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither

import javax.inject.Inject
import models.common._
import play.api.mvc.{ActionRefiner, Results}

import scala.concurrent.ExecutionContext
import services.dredd.DreddClient
import services.rti.ThumbnailRequest
import services.url.PresignedUrlRequest
import utils.UUIDHelper

case class ThumbnailRequestAction @Inject()(
                                             presignedUrlReq: PresignedUrlRequest,
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, ThumbnailRequest]
    with LazyLogging
    with UUIDHelper {

  private final val thumbnailWidth  = 197
  private final val thumbnailHeight = 132

  def refine[A](request: HeimdallRequest[A]) = {
    val result = for {
      file         <- FutureEither.successful(request.media.headOption.toRight(Results.BadRequest))
      presignedUrl <- FutureEither(presignedUrlReq.getUrl(file, request).map(Right(_)))
    } yield {
      ThumbnailRequest(file, presignedUrl, thumbnailWidth, thumbnailHeight, request)
    }
    result.future
  }
}
