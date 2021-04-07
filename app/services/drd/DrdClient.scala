package services.drd

import com.typesafe.config.Config
import java.util.UUID

import com.evidence.service.common.logging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

trait DrdClient {
  def createRedaction(partnerId: UUID, userId: UUID, evidenceId: UUID): Future[WSResponse]
}

@Singleton
class DrdClientImpl @Inject()(config: Config, ws: WSClient)(implicit ex: ExecutionContext) extends DrdClient with LazyLogging {

  def createRedaction(partnerId: UUID, userId: UUID, evidenceId: UUID): Future[WSResponse] = {
    // TODO: Get Evidence Title from evidenceId
    val evidenceTitle = "a test title"

    buildDrdEndpoint(s"/v1/evidences/${evidenceId.toString}/redactions")
      .addHttpHeaders(
        "Partner-Id"  -> partnerId.toString,
        "Authenticated-User-Id"  -> userId.toString,
      )
      .withMethod("POST")
      .withBody(
        Json.obj(
          "EvidenceTitle"  -> evidenceTitle,
        ))
      .execute()
  }

  private def buildDrdEndpoint(endpoint: String) =
    ws.url(config.getString("edc.service.drd.host") + endpoint)
}
