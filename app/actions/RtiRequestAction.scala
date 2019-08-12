package actions

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common._
import play.api.mvc.{ActionRefiner, Results}
import scala.concurrent.ExecutionContext
import services.dredd.DreddClient
import services.rti.RtiRequest
import utils.UUIDHelper

case class RtiRequestAction @Inject()(
  dreddClient: DreddClient
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, RtiRequest]
    with LazyLogging
    with UUIDHelper {

  def refine[A](request: HeimdallRequest[A]) = {

    val result = for {
      file         <- FutureEither.successful(request.media.headOption.toRight(Results.BadRequest))
      presignedUrl <- FutureEither(dreddClient.getUrl(file, request).map(Right(_)))
    } yield {
      RtiRequest(file, presignedUrl, request.watermark, request)
    }
    result.future
  }
}
