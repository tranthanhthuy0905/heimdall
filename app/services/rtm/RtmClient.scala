package services.rtm

import java.net.{URL, URLEncoder}
import java.util.UUID

import akka.http.scaladsl.model.Uri
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

trait RtmClient {
  def send (path: String, agencyId: UUID, evidenceId: UUID, fileId: UUID, args: Map[String, Seq[String]]): Future[WSResponse]
}

@Singleton
class RtmClientImpl @Inject() (dredd: DreddClient,
                               zookeeper: ZookeeperServerSet,
                               ws: WSClient) (implicit ex: ExecutionContext) extends RtmClient with LazyLogging {

  def send (path: String, agencyId: UUID, evidenceId: UUID, fileId: UUID, query: Map[String, Seq[String]]): Future[WSResponse] = {
    for {
      presignedUrl <- dredd.getUrl(agencyId, evidenceId, fileId)
      endpoint <- getEndpoint(fileId)
      request = RtmRequest(path, endpoint, presignedUrl, query)
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

class RtmRequest(path: String, endpoint: ServiceEndpoint, query: Map[String, Seq[String]]) {
  override def toString(): String = {
    def encodeValue(seq: Seq[String]): String = seq.foldLeft("") {
      case ("", value) => URLEncoder.encode(value, "UTF-8")
      case (s, value) => s + "," + URLEncoder.encode(value, "UTF-8")
    }
    val encodedQuery = query.foldLeft("") {
      case ("", (key, value)) => URLEncoder.encode(key, "UTF-8") + "=" + encodeValue(value)
      case (s, (key, value)) => s + "&" + URLEncoder.encode(key, "UTF-8") + "=" + encodeValue(value)
    }
    Uri.from(scheme = "https",
      host = endpoint.host,
      port = endpoint.port,
      path = path,
      queryString = Some(encodedQuery)
    ).toString
  }
}

object RtmRequest {
  def apply(path: String, endpoint: ServiceEndpoint, presignedUrl: URL, query: Map[String, Seq[String]]): String = {
    val request = new RtmRequest(path, endpoint, addToQuery(query, Map("source" -> Seq[String](presignedUrl.toString))))
    request.toString
  }

  private def addToQuery(query: Map[String, Seq[String]], newArg: Map[String, Seq[String]]):  Map[String, Seq[String]] =
    query ++ newArg.map{ case (k,v) => k -> (v ++ query.getOrElse(k,Nil)) }
}
