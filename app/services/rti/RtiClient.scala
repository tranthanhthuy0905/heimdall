package services.rti

import java.net.URL
import java.util.UUID

import com.evidence.service.common.logging.LazyLogging
import com.google.inject.Inject
import com.typesafe.config.Config
import javax.inject.Singleton
import play.api.libs.ws.{WSClient, WSResponse}
import services.rti.Quality.{HighQuality, MediumQuality}

import scala.concurrent.{ExecutionContext, Future}

trait RtiClient {
  def transcode(presignedURL: URL, watermark: String,fileId: UUID): Future[WSResponse]
  def zoom(presignedURL: URL, watermark: String, fileId: UUID): Future[WSResponse]
  def metadata(presignedURL: URL): Future[WSResponse]
}

@Singleton
class RtiClientImpl @Inject()(config: Config, ws: WSClient)(implicit ex: ExecutionContext)
    extends RtiClient
    with LazyLogging {

  def transcode(presignedURL: URL, watermark: String, fileId: UUID): Future[WSResponse] =
    buildTranscodeRequest(presignedURL, watermark)(MediumImage, MediumQuality, fileId).execute()

  /*
   * For better support zoom action, we need an higher quality image from RTI.
   * Therefore `sizeId` and `quality` params should be LargeImage and HighQuality
   */
  def zoom(presignedURL: URL, watermark: String, fileId: UUID): Future[WSResponse] =
    buildTranscodeRequest(presignedURL, watermark)(LargeImage, HighQuality, fileId).execute()

  def metadata(presignedURL: URL): Future[WSResponse] =
    buildMetadataRequest(presignedURL).execute()

  private def buildRTIEndpoint(endpoint: String) = ws.url(config.getString("edc.service.rti.host") + endpoint)

  private def buildTranscodeRequest(presignedURL: URL, watermark: String)(
    size: SizeImage,
    quality: QualityImage,
    fileId: UUID) = {
    buildRTIEndpoint("/v1/images/image")
      .addQueryStringParameters("sizeID" -> size.value)
      .addQueryStringParameters("presignedURL" -> presignedURL.toString)
      .addQueryStringParameters("watermark" -> watermark)
      .addQueryStringParameters("quality" -> quality.value)
      .addQueryStringParameters("identifier" -> fileId.toString)
      .withMethod("GET")
  }

  private def buildMetadataRequest(presignedURL: URL) = {
    buildRTIEndpoint("/v1/images/metadata")
      .addQueryStringParameters("presignedURL" -> presignedURL.toString)
      .withMethod("GET")
  }
}
