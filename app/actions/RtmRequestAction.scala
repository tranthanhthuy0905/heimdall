package actions

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import services.komrade.{KomradeClient, PlaybackFeatures}
import services.rtm.{HeimdallRoutes, RtmQueryHelper, RtmRequest}
import services.url.PresignedUrlClient
import services.zookeeper.HeimdallLoadBalancer

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class RtmRequestAction @Inject()(
     config: Config,
     presignedUrlReq: PresignedUrlClient,
     komradeClient: KomradeClient,
     loadBalancer: HeimdallLoadBalancer,
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, RtmRequest]
    with LazyLogging
    with HeimdallRoutes
    with PlaybackFeatures {

  def refine[A](input: HeimdallRequest[A]) = {

    for {
      isMultiAudioEnabled <- input.getQueryString("partner_id").map(isMultiStreamPlaybackEnabled).getOrElse(Future.successful(false))
      rtmRequest <- buildRtmQueries(input, isMultiAudioEnabled)
    } yield rtmRequest
  }

  private def buildRtmQueries[A](input: HeimdallRequest[A], isMultiAudioEnabled: Boolean) = {
    RtmQueryHelper(input.path, input.queryString, isMultiAudioEnabled).map { rtmQuery =>
      for {
        presignedUrls <- Future.traverse(input.media.toList)(presignedUrlReq.getUrl(_,input))
        endpoint      <- loadBalancer.getInstanceAsFuture(input.media.fileIds.head.toString)
        queries       <- Future.successful(RtmQueryHelper.getRTMQueries(rtmQuery.params, Some(input.watermark), input.playbackSettings, presignedUrls, input.audienceId))
      } yield {
        val uri = Uri
          .from(
            scheme = "https",
            host = endpoint.host,
            port = endpoint.port,
            path = rtmQuery.path,
            queryString = Some(queries)
          )
        Right(new RtmRequest(uri, input.media, presignedUrls, rtmQuery.params, input))
      }
    }.getOrElse(Future.successful(Left(Results.BadRequest)))

  }

  private def isMultiStreamPlaybackEnabled(partnerId: String): Future[Boolean] = {
    komradeClient.listPlaybackPartnerFeatures(partnerId).map(features => features.exists(_.featureName == MultiStreamPlaybackEnabledFeature))
  }
}
