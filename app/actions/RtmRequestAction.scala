package actions

import java.net.URL

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{FileIdent, HeimdallRequest}
import play.api.cache.AsyncCacheApi
import play.api.mvc.{ActionRefiner, Results}
import services.dredd.DreddClient
import services.rtm.{RtmQueryHelper, RtmRequest}
import services.zookeeper.HeimdallLoadBalancer

import scala.concurrent.duration.{Duration, HOURS, MINUTES}
import scala.concurrent.{ExecutionContext, Future}

case class RtmRequestAction @Inject()(
  dredd: DreddClient,
  loadBalancer: HeimdallLoadBalancer,
  cache: AsyncCacheApi
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
  final private val urlExpires = Duration(2, HOURS)
  // starting point: cache url 15 minutes and monitor prod memory usage
  final private val cacheExpired = Duration(15, MINUTES)

  def getUrl[A](file: FileIdent, request: HeimdallRequest[A]): Future[URL] = {
    cache.getOrElseUpdate[URL](file.toString, cacheExpired) {
      dredd.getUrl(file, request, urlExpires.toSeconds.toInt)
    }
  }

  def refine[A](input: HeimdallRequest[A]) = {
    RtmQueryHelper(input.path, input.queryString) match {
      case Some(rtmQuery) =>
        for {
          presignedUrls <- Future.traverse(input.media.toList)(
            getUrl(_, input)
          )
          endpoint <- loadBalancer.getInstanceAsFuture(input.media.fileIds.head.toString)
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
