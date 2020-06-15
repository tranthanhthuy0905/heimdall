package actions

import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common._
import play.api.mvc.{ActionRefiner, Results}
import services.xpreport.playback.XpReportRequest

import scala.concurrent.ExecutionContext

case class XpReportRequestAction @Inject()()(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, XpReportRequest]
{

  def refine[A](request: HeimdallRequest[A]) = {
    val result = for {
      file         <- FutureEither.successful(request.media.headOption.toRight(Results.BadRequest))
    } yield {
      XpReportRequest(file, request)
    }
    result.future
  }
}
