package services.rti

import java.net.URL
import java.util.UUID

import com.evidence.service.common.logging.LazyLogging
import com.google.inject.Inject
import com.typesafe.config.Config
import javax.inject.Singleton
import models.common.FileIdent
import play.api.libs.ws.{WSClient, WSResponse}
import services.rti.Quality.{HighQuality, MediumQuality}

import scala.concurrent.{ExecutionContext, Future}

trait RtiClient {
  def transcode(presignedURL: URL, watermark: String, file: FileIdent): Future[WSResponse]
  def zoom(presignedURL: URL, watermark: String, file: FileIdent): Future[WSResponse]
  def metadata(presignedURL: URL, file: FileIdent): Future[WSResponse]
  def thumbnail(presignedURL: URL, width: Int, height: Int, file: FileIdent): Future[WSResponse]
}

@Singleton
class RtiClientImpl @Inject()(config: Config, ws: WSClient)(implicit ex: ExecutionContext)
    extends RtiClient
    with LazyLogging {

  def transcode(presignedURL: URL, watermark: String, file: FileIdent): Future[WSResponse] =
    buildTranscodeRequest(presignedURL, watermark)(LargeImage, HighQuality, file).stream()

  /*
   * For better support zoom action, we need an higher quality image from RTI.
   * Therefore `sizeId` and `quality` params should be LargeImage and HighQuality
   */
  def zoom(presignedURL: URL, watermark: String, file: FileIdent): Future[WSResponse] =
    buildTranscodeRequest(presignedURL, watermark)(LargeImage, HighQuality, file).stream()

  def metadata(presignedURL: URL, file: FileIdent): Future[WSResponse] =
    buildMetadataRequest(presignedURL, file).stream()

  /*
  * generate image thumbnail at specific dimension
   */
  def thumbnail(presignedURL: URL, width: Int, height: Int, file: FileIdent): Future[WSResponse] =
    buildThumbnailRequest(presignedURL, width, height, file).stream()

  private def buildRTIEndpoint(endpoint: String) = ws.url(config.getString("edc.service.rti.host") + endpoint)

  private def buildTranscodeRequest(
    presignedURL: URL,
    watermark: String)(size: SizeImage, quality: QualityImage, file: FileIdent) = {
    buildRTIEndpoint("/v1/images/image")
      .addQueryStringParameters("sizeID" -> size.value)
      .addQueryStringParameters("presignedURL" -> presignedURL.toString)
      .addQueryStringParameters("watermark" -> watermark)
      .addQueryStringParameters("quality" -> quality.value)
      .addQueryStringParameters("identifier" -> file.fileId.toString)
      .addHttpHeaders(
        "evidenceId" -> file.evidenceId.toString,
        "partnerId" -> file.partnerId.toString,
        "fileId" -> file.fileId.toString,
      )
      .withMethod("GET")
  }

  private def buildThumbnailRequest(presignedURL: URL, width: Int, height: Int, file: FileIdent) = {
    buildRTIEndpoint("/v1/images/thumbnail")
      .addQueryStringParameters("presignedURL" -> presignedURL.toString)
      .addQueryStringParameters("width" -> width.toString)
      .addQueryStringParameters("height" -> height.toString)
      .addHttpHeaders(
        "evidenceId" -> file.evidenceId.toString,
        "partnerId" -> file.partnerId.toString,
        "fileId" -> file.fileId.toString,
      )
      .withMethod("GET")
  }

  private def buildMetadataRequest(presignedURL: URL, file: FileIdent) = {
    buildRTIEndpoint("/v1/images/metadata")
      .addQueryStringParameters("presignedURL" -> presignedURL.toString)
      .addHttpHeaders(
        "evidenceId" -> file.evidenceId.toString,
        "partnerId" -> file.partnerId.toString,
        "fileId" -> file.fileId.toString,
      )
      .withMethod("GET")
  }
}
