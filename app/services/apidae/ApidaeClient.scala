package services.apidae

import com.typesafe.config.Config
import models.common.ZipFileMetadata
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ApidaeClient {
  def transcode(partnerId: UUID, userId: UUID, evidenceId: UUID, fileId: UUID, url: Option[String]): Future[WSResponse]
  def getTranscodingStatus(partnerId: UUID, userId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse]
  def getMediaSummary(partnerId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse]
  def getZipFileInfoRaw(partnerId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse]
  def getZipFileInfo(partnerId: UUID, evidenceId: UUID, fileId: UUID): Future[ZipFileMetadata]

  def getZipStructure(partnerID: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse]
  def getZipStatus(partnerID: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse]
  def requestConcatenate(body: JsValue): Future[WSResponse]
}

@Singleton
class ApidaeClientImpl @Inject()(config: Config, ws: WSClient)(implicit ex: ExecutionContext) extends ApidaeClient {

  def transcode(
    partnerId: UUID,
    userId: UUID,
    evidenceId: UUID,
    fileId: UUID,
    url: Option[String]): Future[WSResponse] =
    buildApidaeEndpoint(s"/files/${fileId.toString}/convert")
      .addQueryStringParameters(
        "partner_id"  -> partnerId.toString,
        "evidence_id" -> evidenceId.toString,
        "user_id"     -> userId.toString,
      )
      .withMethod("PUT")
      .withBody(Json.obj(
        "partner_id"  -> partnerId.toString,
        "evidence_id" -> evidenceId.toString,
        "user_id"     -> userId.toString,
        "file_id"     -> fileId.toString,
        "url"         -> url..fold("")(identity),
      ))
      .execute()

  def getTranscodingStatus(partnerId: UUID, userId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse] =
    buildApidaeEndpoint(s"/files/${fileId.toString}/status")
      .addQueryStringParameters(
        "partner_id"  -> partnerId.toString,
        "evidence_id" -> evidenceId.toString,
        "user_id"     -> userId.toString,
      )
      .withMethod("GET")
      .execute()

  def getMediaSummary(partnerId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse] =
    buildApidaeEndpoint("/v1/media/summary")
      .withQueryStringParameters(
        "partner_id"  -> partnerId.toString,
        "evidence_id" -> evidenceId.toString,
        "file_id"     -> fileId.toString,
      )
      .withMethod("GET")
      .execute()

  def getZipFileInfoRaw(partnerId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse] =
    buildApidaeEndpoint(s"/v1/zip/file")
      .addQueryStringParameters(
        "partner_id"  -> partnerId.toString,
        "evidence_id" -> evidenceId.toString,
        "file_id"     -> fileId.toString,
      )
      .withMethod("GET")
      .execute()

  def getZipFileInfo(partnerId: UUID, evidenceId: UUID, fileId: UUID): Future[ZipFileMetadata] =
    getZipFileInfoRaw(partnerId, evidenceId, fileId)
      .map(response => {
        val fileName = (response.json \ "data" \ "file_name").asOpt[String].getOrElse("")
        val filePath = (response.json \ "data" \ "file_path").asOpt[String].getOrElse("")

        ZipFileMetadata(
          partnerId = partnerId,
          evidenceId = evidenceId,
          fileId = fileId,
          fileName = fileName,
          filePath = filePath
        )
      })

  def getZipStructure(partnerId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse] = {
    buildApidaeEndpoint("/v1/zip/structure")
      .withQueryStringParameters(
        "partner_id"  -> partnerId.toString,
        "evidence_id" -> evidenceId.toString,
        "file_id"     -> fileId.toString,
      )
      .withMethod("GET")
      .execute()
  }

  def getZipStatus(partnerId: UUID, evidenceId: UUID, fileId: UUID): Future[WSResponse] =
    buildApidaeEndpoint("/v1/zip/status")
      .withQueryStringParameters(
        "partner_id"  -> partnerId.toString,
        "evidence_id" -> evidenceId.toString,
        "file_id"     -> fileId.toString,
      )
      .withMethod("GET")
      .execute()

  def requestConcatenate(body: JsValue): Future[WSResponse] =
    buildApidaeEndpoint("/v1/media/video/concatenate")
      .withMethod("POST")
      .withBody(body)
      .execute()

  private def buildApidaeEndpoint(endpoint: String) =
    ws.url(config.getString("edc.service.apidae.host") + endpoint).withHttpHeaders("Content-type" -> "application/json")

}
