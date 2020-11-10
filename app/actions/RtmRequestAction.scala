package actions

import java.net.{URL, URLEncoder}

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import services.dredd.DreddClient
import services.rtm.{RtmQueryHelper, RtmRequest}
import services.zookeeper.HeimdallLoadBalancer

import scala.collection.immutable.Map
import scala.concurrent.{ExecutionContext, Future}

case class RtmRequestAction @Inject()(
  config: Config,
  dredd: DreddClient,
  loadBalancer: HeimdallLoadBalancer
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, RtmRequest]
    with LazyLogging {

  def refine[A](input: HeimdallRequest[A]) = {
    RtmQueryHelper(input.path, input.queryString).map { rtmQuery =>
      for {
        presignedUrls <- Future.traverse(input.media.toList)(dredd.getUrl(_, input))
        endpoint      <- loadBalancer.getInstanceAsFuture(input.media.fileIds.head.toString, input.rtmApiVersion)
        queries       <- Future.successful(getRTMQueries(rtmQuery.params, Some(input.watermark), presignedUrls))
        rtmPath       <- Future.successful(getRTMPath(rtmQuery.path, input.rtmApiVersion, presignedUrls.length > 1))
      } yield {
        val uri = Uri
          .from(
            scheme = "https",
            host = endpoint.host,
            port = endpoint.port,
            path = rtmPath,
            queryString = Some(queries)
          )
        Right(
          new RtmRequest(
            uri,
            input
          ))
      }
    }.getOrElse(Future.successful(Left(Results.BadRequest)))
  }

  private def getRTMPath(path: String, rtmApiVersion: Int, isMulticam: Boolean): String = {
    if (rtmApiVersion == 2 && isMulticam) s"/multicam/$path" else path
  }

  private def getRTMQueries(
    queries: Map[String, String],
    watermark: Option[String],
    presignedUrls: Seq[URL]): String = {
    (Map("source" -> presignedUrls.mkString(",")) ++ watermark
      .map(watermark => queries + ("label" -> watermark))
      .getOrElse(queries)).toSeq.map {
      case (key, value) =>
        URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(
          value,
          "UTF-8"
        )
    }.reduceLeft(_ + "&" + _)
  }
}
