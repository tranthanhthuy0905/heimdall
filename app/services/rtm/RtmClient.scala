package services.rtm

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import com.google.inject.Inject
import com.typesafe.config.Config

import javax.inject.Singleton
import models.common.EmptyMediaIdent
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait RtmClient {
  def send[A](rtmRequest: RtmRequest[A]): Future[WSResponse]
  def probe[A](rtmRequest: RtmRequest[A]): Future[WSResponse]
}

@Singleton
class RtmClientImpl @Inject()(ws: WSClient, config: Config)(implicit ex: ExecutionContext)
    extends RtmClient
    with LazyLogging
    with StrictStatsD {

  final val logLimit = 50
  val logCount       = new AtomicInteger()

  def send[A](rtmRequest: RtmRequest[A]): Future[WSResponse] = {
    // rtm will use these header information for log context and caching
    val header = rtmRequest.media match {
      case EmptyMediaIdent() => Map()
      case media =>
        Map(
          "Partner-Id"  -> media.partnerId.toString,
          "Evidence-Id" -> media.evidenceIds.map(_.toString).mkString(","),
          "File-Id"     -> media.fileIds.map(_.toString).mkString(","),
          "Client-Id"   -> rtmRequest.subjectId
        )
    }

    ws.url(rtmRequest.toString)
      .addHttpHeaders(header.toSeq: _*)
      .get()
  }

  def probe[A](rtmRequest: RtmRequest[A]): Future[WSResponse] = {
    val vmFutureResponse = send(rtmRequest)

    if (config.getBoolean("edc.service.rtm.rtmprobe_als_test")) {
      for {
        k8s <- k8sProbe(rtmRequest)
        vm  <- vmFutureResponse.map(r => r.json.as[JsObject])
      } yield
        if (!k8s.equals(vm)) {
          statsd.increment("ab_testing.response.mismatch")
          if (logCount.incrementAndGet() < logLimit) {
            logger.warn("mismatch between k8s and vm response")(
              "vmResponse"  -> vm,
              "k8sResponse" -> k8s,
            )
          }
        }
    }

    vmFutureResponse
  }

  def k8sProbe[A](rtmRequest: RtmRequest[A]): Future[JsValue] = {
    val json = Json.obj(
      "sources"      -> Json.arr(rtmRequest.getPresignedUrls.map(_.toString)),
      "partner_id"   -> rtmRequest.media.partnerId,
      "evidence_ids" -> rtmRequest.media.evidenceIds.map(_.toString),
      "file_ids"     -> rtmRequest.media.fileIds.map(_.toString),
      "client_id"    -> rtmRequest.subjectId
    ) ++ Json.toJson(rtmRequest.getParams).as[JsObject]

    ws.url(config.getString("edc.service.rtm.host") + "/probe")
      .withRequestTimeout(5.second)
      .withBody(json)
      .withMethod("POST")
      .execute()
      .transform(
        tryResult =>
          Success(
            tryResult.toEither.fold(
              e => {
                statsd.increment("ab_testing.rtmprobe.failed")
                if (logCount.incrementAndGet() < logLimit) {
                  logger.warn("call k8s rtmprobe failed")(
                    "error"       -> e.getMessage,
                    "evidenceIds" -> rtmRequest.media.evidenceIds,
                    "fileIds"     -> rtmRequest.media.fileIds,
                    "partnerId"   -> rtmRequest.media.partnerId,
                  )
                }
                JsNull
              },
              k8s => k8s.json.asOpt[JsObject].getOrElse(JsNull)
            )
        )
      )
  }
}
