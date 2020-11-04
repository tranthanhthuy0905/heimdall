package actions

import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import services.dredd.DreddClient
import services.rtm.{RtmQueryHelper, RtmRequest}
import services.zookeeper.HeimdallLoadBalancer

import scala.concurrent.duration.{Duration, HOURS}
import scala.concurrent.{ExecutionContext, Future}

case class RtmRequestAction @Inject()(
  config: Config,
  dredd: DreddClient,
  loadBalancer: HeimdallLoadBalancer
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, RtmRequest]
    with LazyLogging {

  def refine[A](input: HeimdallRequest[A]) = {
    RtmQueryHelper(input.path, input.queryString) match {
      case Some(rtmQuery) =>
        for {
          presignedUrls <- Future.traverse(input.media.toList)(
            dredd.getUrl(_, input)
          )
          endpoint <- loadBalancer.getInstanceAsFuture(input.media.fileIds.head.toString, input.rtmApiVersion)
        } yield
          Right(
            new RtmRequest(
              input.rtmApiVersion,
              rtmQuery.path,
              endpoint,
              presignedUrls,
              handleWatermark(rtmQuery.params, Some(input.watermark)),
              input
            )
          )
      case None =>
        Future.successful(Left(Results.BadRequest))
    }
  }

  def handleWatermark(query: Map[String, String], watermark: Option[String]): Map[String, String] = {
    watermark match {
      case Some(labelValue) => query + ("label" -> labelValue)
      case None             => query
    }
  }

}
