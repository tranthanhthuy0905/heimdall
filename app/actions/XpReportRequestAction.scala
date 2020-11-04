package actions

import javax.inject.Inject
import models.common._
import play.api.mvc.{ActionRefiner, Results}
import services.xpreport.playback.XpReportRequest

import scala.concurrent.{ExecutionContext, Future}

case class XpReportRequestAction @Inject()()(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, XpReportRequest] {

  def refine[A](request: HeimdallRequest[A]) = {
    Future(
      request.media.headOption
        .map(XpReportRequest(_, request))
        .toRight(Results.BadRequest)
    )
  }
}
