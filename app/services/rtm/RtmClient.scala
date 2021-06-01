package services.rtm

import com.google.inject.Inject
import javax.inject.Singleton
import models.common.EmptyMediaIdent
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration.{Duration, HOURS}
import scala.concurrent.{ExecutionContext, Future}

trait RtmClient {
  def send[A](rtmRequest: RtmRequest[A]): Future[WSResponse]
}

@Singleton
class RtmClientImpl @Inject()(ws: WSClient)(implicit ex: ExecutionContext) extends RtmClient {
  def send[A](rtmRequest: RtmRequest[A]): Future[WSResponse] = {
    // rtm will use these header information for log context and caching
    val header = rtmRequest.media match {
      case EmptyMediaIdent() => Map()
      case media => Map(
        "Partner-Id" -> media.partnerId.toString,
        "Evidence-Id" -> media.evidenceIds.map(_.toString).mkString(","),
        "File-Id" -> media.fileIds.map(_.toString).mkString(","),
        "Client-Id" -> rtmRequest.subjectId
        )
    }

    ws.url(rtmRequest.toString)
      .addHttpHeaders(header.toSeq: _*)
      .get()
  }
}
