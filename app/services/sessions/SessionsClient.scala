package services.sessions

import scala.collection.Set
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import javax.inject.{Inject, Singleton}

import com.evidence.api.thrift.v1.{EntityDescriptor, SessionAuthClient, SessionTokenType}
import com.evidence.service.common.config.Configuration
import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.finagle.FutureConverters._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.sessions.api.thrift.v1._
import com.evidence.service.thrift.v2.{Authorization => RequestAuthorization}

trait SessionsClient {

  def getAuthorization(tokenType: SessionTokenType, token: String): Try[Future[GetAuthorizationResponse]]
  def createSession(subject: EntityDescriptor, audience: EntityDescriptor, subjectScopes: Set[String], tokenType: SessionTokenType,
                    ttlSeconds: Option[Int], userRoleId: Option[Long], authClient: Option[SessionAuthClient]): Try[Future[CreateSessionResponse]]
  def deleteSession(tokenType: SessionTokenType, token: String): Try[Future[Unit]]
}

@Singleton
class SessionsClientImpl @Inject() (implicit ec: ExecutionContext) extends SessionsClient with LazyLogging {

  private val config = Configuration.load()

  private val client: SessionsService.MethodPerEndpoint = {
    val env = FinagleClient.getEnvironment(config)
    val dest = FinagleClient.newThriftUrl(s"com.evidence.service.sessions-service", env, "thrift")
    val client = FinagleClient.newThriftClient().build[SessionsService.MethodPerEndpoint](dest)
    client
  }

  private val auth: RequestAuthorization = RequestAuthorization(
    Option(config.getString("edc.service.sessions.thrift_auth_type")),
    Option(config.getString("edc.service.sessions.thrift_auth_secret")))

  def getAuthorization(tokenType: SessionTokenType, token: String): Try[Future[GetAuthorizationResponse]] = {
    Try(client.getAuthorization(auth, GetAuthorizationRequest(tokenType, token)).toScalaFuture)
  }

  def createSession(subject: EntityDescriptor,
                    audience: EntityDescriptor,
                    subjectScopes: Set[String],
                    tokenType: SessionTokenType,
                    ttlSeconds: Option[Int],
                    userRoleId: Option[Long],
                    authClient: Option[SessionAuthClient]
                    ): Try[Future[CreateSessionResponse]] = {
    Try(client.createSession(auth, CreateSessionRequest(subject, audience, subjectScopes, tokenType, ttlSeconds, userRoleId, authClient)).toScalaFuture)
  }

  def deleteSession(tokenType: SessionTokenType, token: String): Try[Future[Unit]] = {
    Try(client.deleteSession(auth, DeleteSessionRequest(tokenType, token)).toScalaFuture)
  }
}
