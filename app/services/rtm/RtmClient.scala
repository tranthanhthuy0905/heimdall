package services.rtm

import java.util.UUID

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.zookeeper.ServiceEndpoint
import com.google.inject.Inject
import javax.inject.Singleton
import models.common.RtmQueryParams
import play.api.libs.ws.{WSClient, WSResponse}
import services.dredd.DreddClient
import services.zookeeper.ZookeeperServerSet

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait RtmClient {
  def send(query: RtmQueryParams): Future[WSResponse]
}

@Singleton
class RtmClientImpl @Inject()(dredd: DreddClient,
                              zookeeper: ZookeeperServerSet,
                              ws: WSClient)(implicit ex: ExecutionContext) extends RtmClient with LazyLogging {

  def send(rtmQuery: RtmQueryParams): Future[WSResponse] = {
    for {
      presignedUrls <- Future.traverse(rtmQuery.media.toList)(dredd.getUrl)
      endpoint <- getEndpoint(rtmQuery.media.fileIds.head)
      request = RtmRequest(rtmQuery.path, endpoint, presignedUrls, rtmQuery.params)
      response <- ws.url(request).withMethod("GET").stream
    } yield response
  }

  private def getEndpoint(fileId: UUID): Future[ServiceEndpoint] = {
    zookeeper.getInstance(fileId.toString) match {
      case Success(server) => Future.successful(server)
      case Failure(exception) => Future.failed(exception)
    }
  }

}
