package services.janus

import com.typesafe.config.Config
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

trait JanusClient {
  def transcode(partnerId: UUID, userId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse]
  def getTranscodingStatus(partnerId: UUID, userId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse]
}

@Singleton
class JanusClientImpl @Inject()(config: Config, ws: WSClient)(implicit ex: ExecutionContext) extends JanusClient {

  def transcode(partnerId: UUID, userId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse] =
    buildJanusEndpoint(s"/files/${fileId.toString}/convert")
      .addQueryStringParameters("partner_id" -> partnerId.toString)
      .addQueryStringParameters("evidence_id" -> evidenceId.toString)
      .addQueryStringParameters("user_id" -> userId.toString)
      .withMethod("PUT")
      .withBody(
        Json.obj(
          "partnerId"   -> partnerId.toString,
          "evidence_id" -> evidenceId.toString,
          "user_id"     -> userId.toString,
          "file_id"     -> fileId.toString
        ))
      .execute()

  def getTranscodingStatus(partnerId: UUID, userId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse] =
    buildJanusEndpoint(s"/files/${fileId.toString}/status")
      .addQueryStringParameters("partner_id" -> partnerId.toString)
      .addQueryStringParameters("evidence_id" -> evidenceId.toString)
      .addQueryStringParameters("user_id" -> userId.toString)
      .withMethod("GET")
      .execute()

  private def buildJanusEndpoint(endpoint: String) =
    ws.url(config.getString("edc.service.janus.host") + endpoint).withHttpHeaders("Content-type" -> "application/json")

}
