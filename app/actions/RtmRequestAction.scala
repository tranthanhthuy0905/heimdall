package actions

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import services.dredd.DreddClient
import services.rtm.{RtmQueryHelper, RtmRequest}
import services.zookeeper.ZookeeperServerSet

import scala.concurrent.{ExecutionContext, Future}

case class RtmRequestAction @Inject()(
  dredd: DreddClient,
  zookeeper: ZookeeperServerSet
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
          endpoint <- zookeeper.getInstanceAsFuture(input.media.fileIds.head)
        } yield
          Right(
            new RtmRequest(
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
