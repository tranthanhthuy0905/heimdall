package services.dredd

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.evidence.api.thrift.v1.TidEntities
import com.evidence.service.common.config.Configuration
import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.dredd.thrift._

trait DreddClient {
  def getPresignedUrl2 (agencyId: UUID, evidenceId: UUID, fileId: UUID, expiresSecs: Int = 60) : Try[Future[PresignedUrlResponse]]
}

@Singleton
class DreddClientImpl @Inject() (implicit ex: ExecutionContext) extends DreddClient with LazyLogging {

  private val config = Configuration.load()

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

  override def getPresignedUrl2 (agencyId: UUID, evidenceId: UUID, fileId: UUID, expiresSecs: Int) : Try[Future[PresignedUrlResponse]] = {
    import com.evidence.service.common.finagle.FutureConverters._

    val ip = "127.0.0.1" // TODO: add request context (see lantern)
    val dreddUpdatedBy = Tid(TidEntities.valueOf("AuthClient"), None, None) // TODO: add subjectId, and subjectDomain
    val requestContext = RequestContext(dreddUpdatedBy, Option(ip))

    val request = PresignedUrlRequest(
      partnerId = agencyId.toString,
      evidenceId = evidenceId.toString,
      fileId = fileId.toString,
      expiresSecs = expiresSecs,
      suppressAudit = Some(true)) // TODO: pass suppressAudit value, which shall default to false
    Try(client.getPresignedUrl2(auth, request, requestContext).toScalaFuture)
  }

}

// TODO: add tests