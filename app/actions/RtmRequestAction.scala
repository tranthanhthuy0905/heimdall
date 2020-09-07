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

  // We have a fix in the latern service to extends the presigned url to 2 hours. However, @dkong reported the issue still
  // The issue happens when the presigned URL that is used to serve the evidence file to RTM is expired.
  // happening and we found the root cause of this issue relates to the mio.range-cacher.
  // The mio.RangeCacher extract the file as the cache key => multiple presigned URLs of same evidence file will have same
  // cache key. In the other hand, the cacher assumes the newer URL will have longer expiration than the older one, So it
  // always uses the newer URL for a cache key.
  // The logic above causes the problem when the Presigned URL which was generated from the Heimdall (when the user views
  // the evidence on EDP) has shorter expiration (60s) than the presigned URL which was generated by Lantern (7200s).
  // EVP-587 2hrs expiration. https://git.taservs.net/ecom/lantern/pull/2064/files
  final private val ttl = Duration(2, HOURS)

  def refine[A](input: HeimdallRequest[A]) = {
    RtmQueryHelper(input.path, input.queryString) match {
      case Some(rtmQuery) =>
        for {
          presignedUrls <- Future.traverse(input.media.toList)(
            dredd.getUrl(_, input, ttl)
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
