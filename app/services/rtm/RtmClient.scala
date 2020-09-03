package services.rtm

import com.google.inject.Inject
import javax.inject.Singleton
import models.common.EmptyMediaIdent
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration.{Duration, HOURS}
import scala.concurrent.{ExecutionContext, Future}

trait RtmClient {
  def send[A](rtmRequest: RtmRequest[A]): Future[WSResponse]
  def stream(rtmRequestString: String): Future[WSResponse]
}

@Singleton
class RtmClientImpl @Inject()(ws: WSClient)(implicit ex: ExecutionContext) extends RtmClient {
  def send[A](rtmRequest: RtmRequest[A]): Future[WSResponse] = {
    // previous build header from MediaIdent class but now build header here because
    // rtmv2 need media_id vs client_id which use audienceId
    val header = rtmRequest.media match {
      case EmptyMediaIdent() => Map()
      case media => Map(
        "file_id" -> media.fileIds.map(_.toString).mkString(","),
        "partner_id" -> media.partnerId.toString,
        "media_id" -> (media.partnerId.toString + "_" +  media.fileIds.map(_.toString).mkString("_")),
        "client_id" -> ( media.partnerId.toString + "_" +  rtmRequest.subjectId)
        )
    }

    ws.url(rtmRequest.toString)
      .addHttpHeaders(header.toSeq: _*)
      .get()
  }

  def stream(rtmRequestString: String): Future[WSResponse] = {
    ws.url(rtmRequestString)
      .withMethod("GET")
      .withRequestTimeout(Duration(2, HOURS)) // FIXME: unify with presigned url TTL
      .get()
  }
}
