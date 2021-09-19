package services.drd

import java.util.UUID

import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

trait DrdClient {

  def call(
    endpoint: String,
    method: String,
    userId: UUID,
    userPartnerId: UUID,
    body: Option[JsValue],
    clientIpAddress: String): Future[WSResponse]
}

@Singleton
class DrdClientImpl @Inject()(config: Config, ws: WSClient)(implicit ex: ExecutionContext)
    extends DrdClient
    with LazyLogging {

  def call(
    endpoint: String,
    method: String,
    userId: UUID,
    userPartnerId: UUID,
    body: Option[JsValue],
    clientIpAddress: String) = {
    ws.url(config.getString("edc.service.drd.host") + endpoint)
      .addHttpHeaders(
        "Partner-Id"            -> userPartnerId.toString,
        "Authenticated-User-Id" -> userId.toString,
        "Client-Ip"             -> clientIpAddress,
        "Accept"                -> "application/json",
      )
      .withMethod(method)
      .withBody(body.getOrElse(JsObject.empty))
      .withRequestTimeout(30.second)
      .execute()
  }
}
