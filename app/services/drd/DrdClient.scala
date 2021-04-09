package services.drd

import com.typesafe.config.Config
import java.util.UUID

import com.evidence.service.common.logging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

trait DrdClient {
  def call(endpoint: String, method: String, partnerId: UUID, userId: UUID, body: Option[JsValue]): Future[WSResponse]
}

@Singleton
class DrdClientImpl @Inject()(config: Config, ws: WSClient)(implicit ex: ExecutionContext)
    extends DrdClient
    with LazyLogging {

  def call(endpoint: String, method: String, partnerId: UUID, userId: UUID, body: Option[JsValue]) = {
    ws.url(config.getString("edc.service.drd.host") + endpoint)
      .addHttpHeaders(
        "Partner-Id"            -> partnerId.toString,
        "Authenticated-User-Id" -> userId.toString,
        "Accept"                -> "application/json",
      )
      .withMethod(method)
      .withBody(body.getOrElse(JsObject.empty))
      .withRequestTimeout(30.second)
      .execute()
  }
}
