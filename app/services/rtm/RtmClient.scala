package services.rtm

import java.util.UUID

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.zookeeper.ServiceEndpoint
import com.google.inject.Inject
import javax.inject.Singleton
import play.api.libs.ws.{WSClient, WSResponse}
import services.dredd.DreddClient
import services.zookeeper.ZookeeperServerSet

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import models.common.ValidatedQuery

trait RtmClient {
  def send(path: String, query: ValidatedQuery): Future[WSResponse]
}

@Singleton
class RtmClientImpl @Inject()(dredd: DreddClient,
                              zookeeper: ZookeeperServerSet,
                              ws: WSClient)(implicit ex: ExecutionContext) extends RtmClient with LazyLogging {


  def send(path: String, query: ValidatedQuery): Future[WSResponse] = {
    for {
      presignedUrl <- dredd.getUrl(query.file.partnerId, query.file.evidenceId, query.file.fileId)
      endpoint <- getEndpoint(query.file.fileId)
      request = RtmRequest(path, endpoint, presignedUrl, query.params)
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
