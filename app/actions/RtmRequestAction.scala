package actions

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import services.dredd.DreddClient
import services.komrade.KomradeClient
import services.rtm.{HeimdallRoutes, RtmQueryHelper, RtmRequest}
import services.zookeeper.HeimdallLoadBalancer

import scala.concurrent.{ExecutionContext, Future}

case class RtmRequestAction @Inject()(
  config: Config,
  dredd: DreddClient,
  komrade: KomradeClient,
  loadBalancer: HeimdallLoadBalancer,
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, RtmRequest]
    with LazyLogging
    with HeimdallRoutes {

  def refine[A](input: HeimdallRequest[A]) = {
    RtmQueryHelper(input.path, input.queryString).map { rtmQuery =>
      for {
        presignedUrls <- Future.traverse(input.media.toList)(dredd.getUrl(_, input))
        endpoint      <- loadBalancer.getInstanceAsFuture(input.media.fileIds.head.toString)
        queries       <- Future(RtmQueryHelper.getRTMQueries(rtmQuery.params, Some(input.watermark), input.playbackSettings, presignedUrls, input.audienceId))
      } yield {
        val uri = Uri
          .from(
            scheme = "https",
            host = endpoint.host,
            port = endpoint.port,
            path = rtmQuery.path,
            queryString = Some(queries)
          )
        Right(new RtmRequest(uri, input.media, input))
      }
    }.getOrElse(Future.successful(Left(Results.BadRequest)))
  }
}
