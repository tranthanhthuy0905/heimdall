package services.rti

import com.evidence.service.common.logging.LazyLogging
import com.google.inject.Inject
import com.typesafe.config.Config
import javax.inject.Singleton
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

trait RtiClient {
  def getImage(presignedURL: String, watermark: String): Future[WSResponse]
}

@Singleton
class RtiClientImpl @Inject()(config: Config, ws: WSClient)(implicit ex: ExecutionContext) extends RtiClient with LazyLogging {
  def getImage(presignedURL: String, watermark: String): Future[WSResponse] = {
    ws.url(config.getString("edc.service.rti.host") + "/v1/images/image")
      .addQueryStringParameters("sizeID" -> "")
      .addQueryStringParameters("presignedURL" -> presignedURL)
      .addQueryStringParameters("watermark" -> watermark)
      .withMethod("GET")
      .execute()
  }
}