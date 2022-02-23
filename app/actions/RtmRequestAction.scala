package actions

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config

import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import services.dredd.DreddClient
import services.komrade.{KomradeClient, PlaybackFeatures}
import services.rtm.{HeimdallRoutes, RtmQueryHelper, RtmRequest}
import services.zookeeper.HeimdallLoadBalancer

import scala.concurrent.{ExecutionContext, Future}

case class RtmRequestAction @Inject()(
  config: Config,
  dredd: DreddClient,
  komradeClient: KomradeClient,
  loadBalancer: HeimdallLoadBalancer,
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, RtmRequest]
    with LazyLogging
    with HeimdallRoutes
    with PlaybackFeatures {

  def refine[A](input: HeimdallRequest[A]) = {
    val isMultiAudioEnabled =  isMultiStreamPlaybackEnabled(input.getQueryString("partner_id"))
    RtmQueryHelper(input.path, input.queryString, isMultiAudioEnabled).map { rtmQuery =>
      for {
        presignedUrls <- Future.traverse(input.media.toList)(dredd.getUrl(_, input))
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

  def isMultiStreamPlaybackEnabled(partnerId: Option[String]): Boolean = {
    partnerId.map(komradeClient.listPlaybackPartnerFeatures).map(resp => {
      resp.map(features => features.exists(_.featureName == MultiStreamPlaybackEnabledFeature))
    }).getOrElse(Future.successful(false)).foreach(found => {
      if (found) {
        true
      }
    })
    false
  }
}
