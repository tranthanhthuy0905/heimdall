package services.rtm

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration.{Duration, HOURS}
import scala.concurrent.{ExecutionContext, Future}

trait RtmClient {
  def send(rtmRequestString: String): Future[WSResponse]
  def stream(rtmRequestString: String): Future[WSResponse]
}

@Singleton
class RtmClientImpl @Inject()(ws: WSClient)(implicit ex: ExecutionContext) extends RtmClient {

  def send(rtmRequestString: String): Future[WSResponse] = {
    ws.url(rtmRequestString).get()
  }

  def stream(rtmRequestString: String): Future[WSResponse] = {
    ws.url(rtmRequestString)
      .withMethod("GET")
      .withRequestTimeout(Duration(2, HOURS)) // FIXME: unify with presigned url TTL
      .get()

  }
}
