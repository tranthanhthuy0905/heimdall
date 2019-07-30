package services.rtm

import com.evidence.service.common.logging.LazyLogging
import com.google.inject.Inject
import javax.inject.Singleton
import play.api.libs.ws.{WSClient, WSResponse}
import services.dredd.DreddClient
import services.zookeeper.ZookeeperServerSet

import scala.concurrent.{ExecutionContext, Future}

trait RtmClient {
  def send(rtmRequest: String): Future[WSResponse]
}

@Singleton
class RtmClientImpl @Inject()(dredd: DreddClient,
                              zookeeper: ZookeeperServerSet,
                              ws: WSClient)(implicit ex: ExecutionContext) extends RtmClient with LazyLogging {

  def send(rtmRequest: String): Future[WSResponse] = {
    ws.url(rtmRequest).get()
  }
}
