package services.dredd

import java.net.URL
import java.util.UUID

import com.evidence.api.thrift.v1.TidEntities
import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.dredd.thrift._
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

trait DreddClient {
  def getUrl (agencyId: UUID, evidenceId: UUID, fileId: UUID, expiresSecs: Int = 60) : Future[URL]
}

@Singleton
class DreddClientImpl @Inject() (config: Config) (implicit ex: ExecutionContext) extends DreddClient with LazyLogging {

  private val client: DreddService.MethodPerEndpoint = {
    val env = FinagleClient.getEnvironment(config)
    val dest = FinagleClient.newThriftUrl(s"com.evidence.service.dredd-service", env, "thrift")
    val client = FinagleClient.newThriftClient().build[DreddService.MethodPerEndpoint](dest)
    client
  }

  private val auth: Authorization = {
    val secret = config.getString("edc.service.dredd.thrift_auth_secret")
    val authType = config.getString("edc.service.dredd.thrift_auth_type")
    Authorization(authType, secret)
  }

  override def getUrl (agencyId: UUID, evidenceId: UUID, fileId: UUID, expiresSecs: Int) : Future[URL] = {
    def extractUrlFromDreddResponse(r: PresignedUrlResponse): Future[URL] = {
      val u = for {
        presigned <- r.presignedUrl
        urlString <- presigned.url
        url = new URL(urlString)
      } yield Future(url)
      u.getOrElse {
        logger.error("noUrlInResponse")(
          "message" -> "No URL contained in PresignedUrlResponse", "evidenceId" -> evidenceId, "agencyId" -> agencyId)
          Future.failed(new Exception("No URL contained in PresignedUrlResponse for " +
            s"evidenceId=$evidenceId partnerId=$agencyId"))
      }
    }
    for {
      presignedUrlResponse <- getPresignedUrl(agencyId, evidenceId, fileId, expiresSecs)
      url <- extractUrlFromDreddResponse(presignedUrlResponse)
    } yield url
  }

  private def getPresignedUrl (agencyId: UUID, evidenceId: UUID, fileId: UUID, expiresSecs: Int) : Future[PresignedUrlResponse] = {
    import com.evidence.service.common.finagle.FutureConverters._

    val ip = "127.0.0.1" // TODO: add request context (see lantern)
    val dreddUpdatedBy = Tid(TidEntities.valueOf("AuthClient"), None, None) // TODO: add subjectId, and subjectDomain
    val requestContext = RequestContext(dreddUpdatedBy, Option(ip))

    val request = PresignedUrlRequest(
      partnerId = agencyId.toString,
      evidenceId = evidenceId.toString,
      fileId = fileId.toString,
      expiresSecs = expiresSecs,
      suppressAudit = Some(true))
    client.getPresignedUrl2(auth, request, requestContext).toScalaFuture
  }

}
