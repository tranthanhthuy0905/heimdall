package services.apidae

import com.typesafe.config.Config
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

trait ApidaeClient {
  def transcode(partnerId: UUID, userId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse]
  def getTranscodingStatus(partnerId: UUID, userId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse]
}

@Singleton
class ApidaeClientImpl @Inject()(config: Config, ws: WSClient)(implicit ex: ExecutionContext) extends ApidaeClient {

  def transcode(partnerId: UUID, userId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse] =
    buildApidaeEndpoint(s"/files/${fileId.toString}/convert")
      .addQueryStringParameters("partner_id" -> partnerId.toString)
      .addQueryStringParameters("evidence_id" -> evidenceId.toString)
      .addQueryStringParameters("user_id" -> userId.toString)
      .withMethod("PUT")
      .withBody(
        Json.obj(
          "partner_id"  -> partnerId.toString,
          "evidence_id" -> evidenceId.toString,
          "user_id"     -> userId.toString,
          "file_id"     -> fileId.toString
        ))
      .execute()

  def getTranscodingStatus(partnerId: UUID, userId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse] =
    buildApidaeEndpoint(s"/files/${fileId.toString}/status")
      .addQueryStringParameters("partner_id" -> partnerId.toString)
      .addQueryStringParameters("evidence_id" -> evidenceId.toString)
      .addQueryStringParameters("user_id" -> userId.toString)
      .withMethod("GET")
      .execute()

  def getZipFileInfo(partnerId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse] =
    buildApidaeEndpoint(s"/v1/zip/file")
      .addQueryStringParameters("partner_id" -> partnerId.toString)
      .addQueryStringParameters("evidence_id" -> evidenceId.toString)
      .addQueryStringParameters("file_id" -> fileId.toString)
      .withMethod("GET")
      .execute()

  private def buildApidaeEndpoint(endpoint: String) =
    ws.url(config.getString("edc.service.apidae.host") + endpoint).withHttpHeaders("Content-type" -> "application/json")

}
