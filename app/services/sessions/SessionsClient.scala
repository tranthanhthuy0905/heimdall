package services.sessions

import com.evidence.api.thrift.v1.{EntityDescriptor, SessionAuthClient, SessionTokenType}
import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.finagle.FutureConverters._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.sessions.api.thrift.v1._
import com.evidence.service.thrift.v2.{Authorization => RequestAuthorization}
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}

import scala.collection.Set
import scala.concurrent.{ExecutionContext, Future}

trait SessionsClient {
  def getAuthorization(tokenType: SessionTokenType, token: String): Future[GetAuthorizationResponse]
  def createSession(subject: EntityDescriptor, audience: EntityDescriptor, subjectScopes: Set[String], tokenType: SessionTokenType,
                    ttlSeconds: Option[Int], userRoleId: Option[Long], authClient: Option[SessionAuthClient]): Future[CreateSessionResponse]
  def deleteSession(tokenType: SessionTokenType, token: String): Future[Unit]
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

  def createSession(subject: EntityDescriptor,
                    audience: EntityDescriptor,
                    subjectScopes: Set[String],
                    tokenType: SessionTokenType,
                    ttlSeconds: Option[Int],
                    userRoleId: Option[Long],
                    authClient: Option[SessionAuthClient]
                    ): Future[CreateSessionResponse] = {
    client.createSession(auth, CreateSessionRequest(subject, audience, subjectScopes, tokenType, ttlSeconds, userRoleId, authClient)).toScalaFuture
  }

  def deleteSession(tokenType: SessionTokenType, token: String): Future[Unit] = {
    client.deleteSession(auth, DeleteSessionRequest(tokenType, token)).toScalaFuture
  }
}
