package actions

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import models.common._
import play.api.mvc.{ActionRefiner, Results}
import services.rti.RtiRequest
import services.url.PresignedUrlClient
import utils.UUIDHelper

import javax.inject.Inject
import scala.concurrent.ExecutionContext

case class RtiRequestAction @Inject()(
                                       presignedUrlReq: PresignedUrlClient,
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, RtiRequest]
    with LazyLogging
    with UUIDHelper {

  def refine[A](request: HeimdallRequest[A]) = {

    val result = for {
      file         <- FutureEither.successful(request.media.headOption.toRight(Results.BadRequest))
      presignedUrl <- FutureEither(presignedUrlReq.getUrl(file, request).map(Right(_)))
    } yield {
      RtiRequest(file, presignedUrl, request.watermark, request)
    }
    result.future
  }
}
