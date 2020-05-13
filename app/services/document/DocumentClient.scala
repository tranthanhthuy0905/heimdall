package services.document

import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import java.net.URL

import javax.inject.{Inject, Singleton}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

trait DocumentClient {
  def view(presignedURL: URL): Future[WSResponse]
}

@Singleton
class DocumentClientImpl @Inject()(config: Config, ws: WSClient)(implicit ex: ExecutionContext)
    extends DocumentClient
    with LazyLogging {

  def view(presignedURL: URL): Future[WSResponse] = {
    ws.url(presignedURL.toString)
      .withMethod("GET")
      .withRequestTimeout(Duration.Inf)
      .stream()
  }

}
