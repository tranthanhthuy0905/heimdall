package services.sessions

import com.evidence.api.thrift.v1.SessionTokenType
import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.finagle.FutureConverters._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.sessions.api.thrift.v1._
import com.evidence.service.thrift.v2.{Authorization => RequestAuthorization}
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

trait SessionsClient {
  def getAuthorization(tokenType: SessionTokenType, token: String): Future[GetAuthorizationResponse]
}

@Singleton
class SessionsClientImpl @Inject()(config: Config)(implicit ec: ExecutionContext) extends SessionsClient with LazyLogging {

  private val client: SessionsService.MethodPerEndpoint = {
    val env = FinagleClient.getEnvironment(config)
    val dest = FinagleClient.newThriftUrl("com.evidence.service.sessions-service", env, "thrift")
    val client = FinagleClient.newThriftClient().build[SessionsService.MethodPerEndpoint](dest)
    client
  }

  private val auth: RequestAuthorization = RequestAuthorization(
    Option(config.getString("edc.service.sessions.thrift_auth_type")),
    Option(config.getString("edc.service.sessions.thrift_auth_secret")))

  def getAuthorization(tokenType: SessionTokenType, token: String): Future[GetAuthorizationResponse] = {
    client.getAuthorizationWithoutExtending(auth, GetAuthorizationRequest(tokenType, token)).toScalaFuture
  }
}
