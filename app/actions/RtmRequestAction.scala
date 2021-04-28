package actions

import java.net.{URL, URLEncoder}
import java.util.UUID

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.komrade.thrift.{WatermarkPosition, WatermarkSetting}
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
        rtmApiVersion <- Future.successful(routeSplitter.getApiVersion(input.media.fileIds.headOption.getOrElse(UUID.randomUUID), input.rtmApiVersion))
        endpoint      <- loadBalancer.getInstanceAsFuture(input.media.fileIds.head.toString, rtmApiVersion)
        isRequestingMaster <- Future.successful(input.path.startsWith(hlsMaster) || input.path.startsWith(hlsMasterV2))
        watermarkSettings: Option[WatermarkSetting] <- if (isRequestingMaster) komrade.getWatermarkSettings(input.audienceId) else Future(None)
        queries       <- Future.successful(getRTMQueries(rtmQuery.params, Some(input.watermark), presignedUrls, watermarkSettings))
        rtmPath       <- Future.successful(getRTMPath(rtmQuery.path, rtmApiVersion, presignedUrls.length > 1))
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
            rtmApiVersion,
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
                             presignedUrls: Seq[URL],
                             watermarkSettings: Option[WatermarkSetting]): String = {
    val presignedUrlsMap = Map("source" -> presignedUrls.mkString(","))
    var watermarkMap = watermark
      .map(watermark => queries + ("label" -> watermark))
      .getOrElse(queries)
    watermarkMap = watermarkSettings
      .map(settings => watermarkMap + ("lp" -> settings.position.value.toString))
      .getOrElse(watermarkMap)

    (presignedUrlsMap ++ watermarkMap).toSeq.map {
      case (key, value) =>
        URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(
          value,
          "UTF-8"
        )
    }.reduceLeft(_ + "&" + _)
  }
}
