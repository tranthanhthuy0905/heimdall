package services.rtm

import java.util.UUID

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.zookeeper.ServiceEndpoint
import com.google.inject.Inject
import javax.inject.Singleton
import play.api.libs.ws.{WSClient, WSResponse}
import services.dredd.DreddClient
import services.zookeeper.ZookeeperServerSet

import scala.collection.immutable.Map
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class FileIdent (fileId: String, evidenceId: String, partnerId: String) {
  override def toString: String = {
    s"file_id=${fileId}&evidence_id=${evidenceId}&partner_id=${partnerId}"
  }
}

trait RtmClient {
  def send (path: String, file: FileIdent, args: Map[String, Seq[String]]): Future[WSResponse]
}

@Singleton
class RtmClientImpl @Inject() (dredd: DreddClient,
                               zookeeper: ZookeeperServerSet,
                               ws: WSClient) (implicit ex: ExecutionContext) extends RtmClient with LazyLogging {

  def send (path: String, file: FileIdent, query: Map[String, Seq[String]]): Future[WSResponse] = {
    val partnerId = Convert.tryToUuid(file.partnerId)
    val evidenceId = Convert.tryToUuid(file.evidenceId)
    val fileId = Convert.tryToUuid(file.fileId)
    (partnerId, evidenceId, fileId) match {
      case (Some(partnerId), Some(evidenceId), Some(fileId)) =>
        for {
          presignedUrl <- dredd.getUrl(partnerId, evidenceId, fileId)
          endpoint <- getEndpoint(fileId)
          request = RtmRequest(path, endpoint, presignedUrl, query)
          response <- ws.url(request).withMethod("GET").stream
        } yield response
      case _ =>
        Future.failed(new Exception(s"Malformed file identifier(s), expected identifier(s) to be UUID convertible, file=${file.toString}"))
    }
  }

  private def getEndpoint(fileId: UUID): Future[ServiceEndpoint] = {
    zookeeper.getInstance(fileId.toString) match {
      case Success(server) => Future.successful(server)
      case Failure(exception) => Future.failed(exception)
    }
  }

}
