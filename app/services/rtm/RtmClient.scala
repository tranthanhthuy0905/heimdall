package services.rtm

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import com.google.inject.Inject
import com.typesafe.config.Config

import javax.inject.Singleton
import models.common.EmptyMediaIdent
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.{WSClient, WSResponse}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ExecutionContext, Future}

trait RtmClient {
  def send[A](rtmRequest: RtmRequest[A]): Future[WSResponse]
  def probe[A](rtmRequest: RtmRequest[A]): Future[WSResponse]
}

@Singleton
class RtmClientImpl @Inject()(ws: WSClient, config: Config)(implicit ex: ExecutionContext)
    extends RtmClient
    with LazyLogging
    with StrictStatsD {

  final val probePath = config.getString("edc.service.rtm.host") + "/probe"

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
    val json = Json.obj(
      "sources"      -> rtmRequest.getPresignedUrls.map(_.toString),
      "partner_id"   -> rtmRequest.media.partnerId,
      "evidence_ids" -> rtmRequest.media.evidenceIds.map(_.toString),
      "file_ids"     -> rtmRequest.media.fileIds.map(_.toString),
      "client_id"    -> rtmRequest.subjectId
    ) ++ Json.toJson(rtmRequest.getParams).as[JsObject]

    ws.url(probePath)
      .withBody(json)
      .withMethod("POST")
      .withHttpHeaders(("Content-Type", "application/json"))
      .execute()
  }
}
