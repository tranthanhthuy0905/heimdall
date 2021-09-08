package services.metadata

import com.evidence.service.common.logging.LazyLogging
import com.google.inject.Inject
import com.typesafe.config.Config
import models.common.FileIdent
import play.api.libs.ws.{WSClient, WSResponse}

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

trait MetadataClient {
  def getMetadata(file: FileIdent, version: String): Future[WSResponse]
}

@Singleton
class MetadataClientImpl @Inject()(config: Config, ws: WSClient)(implicit ex: ExecutionContext)
    extends MetadataClient
    with LazyLogging {

  def getMetadata(file: FileIdent, version: String): Future[WSResponse] = {
    ws.url(config.getString("edc.service.metadata.host") + "/metadata")
      .addQueryStringParameters(
        "partner_id"  -> file.partnerId.toString,
        "evidence_id" -> file.evidenceId.toString,
        "file_id"     -> file.fileId.toString,
        "version"     -> version
      )
      .withMethod("GET")
      .execute()
  }
}
