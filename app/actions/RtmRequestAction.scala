package actions

import java.net.{URL, URLEncoder}
import java.util.UUID

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import services.dredd.DreddClient
import services.komrade.KomradeClient
import services.routesplitter.RouteSplitter
import services.rtm.{HeimdallRoutes, RtmQueryHelper, RtmRequest}
import services.zookeeper.HeimdallLoadBalancer

import scala.collection.immutable.Map
import scala.concurrent.{ExecutionContext, Future}

case class RtmRequestAction @Inject()(
  config: Config,
  dredd: DreddClient,
  komrade: KomradeClient,
  loadBalancer: HeimdallLoadBalancer,
  routeSplitter: RouteSplitter
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, RtmRequest]
    with LazyLogging
    with HeimdallRoutes {

  def refine[A](input: HeimdallRequest[A]) = {
    RtmQueryHelper(input.path, input.queryString).map { rtmQuery =>
      for {
        presignedUrls <- Future.traverse(input.media.toList)(dredd.getUrl(_, input))
        endpoint      <- loadBalancer.getInstanceAsFuture(input.media.fileIds.head.toString)
        isRequestingMaster <- Future.successful(input.path.startsWith(hlsMaster) || input.path.startsWith(hlsMasterV2))
        queries       <- getRTMQueries(rtmQuery.params, Some(input.watermark), presignedUrls, isRequestingMaster, input.audienceId)
      } yield {
        val uri = Uri
          .from(
            scheme = "https",
            host = endpoint.host,
            port = endpoint.port,
            path = rtmQuery.path,
            queryString = Some(queries)
          )
        Right(new RtmRequest(uri, input))
      }
    }.getOrElse(Future.successful(Left(Results.BadRequest)))
  }

  private def getRTMQueries(
                             queries: Map[String, String],
                             watermark: Option[String],
                             presignedUrls: Seq[URL],
                             isRequestingMaster: Boolean,
                             partnerId: String): Future[String] = {
    val presignedUrlsMap = Map("source" -> presignedUrls.mkString(","))
    var watermarkMap = watermark
      .map(watermark => queries + ("label" -> watermark))
      .getOrElse(queries)
    if (isRequestingMaster){
      komrade.getWatermarkSettings(partnerId).map(watermarkSettings => {
        watermarkMap = watermarkMap + ("lp" -> watermarkSettings.position.value.toString)
        buildQueryParams(presignedUrlsMap ++ watermarkMap)
      })
    } else Future.successful(buildQueryParams(presignedUrlsMap ++ watermarkMap))
  }

  private def buildQueryParams(map: Map[String, String]): String = {
    map.toSeq.map {
      case (key, value) =>
        URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(
          value,
          "UTF-8"
        )
    }.reduceLeft(_ + "&" + _)
  }
}
