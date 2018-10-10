package services.sessions

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import com.evidence.api.thrift.v1.{EntityDescriptor, SessionAuthClient, SessionTokenType}
import com.evidence.service.common.config.Configuration
import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.finagle.FutureConverters._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.sessions.api.thrift.v1._
import com.evidence.service.thrift.v2.{Authorization => RequestAuthorization}
import scala.util.Try
import scala.collection.Set

trait SessionsClient {

  def getAuthorization(tokenType: SessionTokenType, token: String): Try[Future[GetAuthorizationResponse]]
  def createSession(subject: EntityDescriptor, audience: EntityDescriptor, subjectScopes: Set[String], tokenType: SessionTokenType,
                    ttlSeconds: Option[Int], userRoleId: Option[Long], authClient: Option[SessionAuthClient]): Try[Future[CreateSessionResponse]]
  def deleteSession(tokenType: SessionTokenType, token: String): Try[Future[Unit]]
}

@Singleton
class SessionsClientImpl @Inject() (implicit ec: ExecutionContext) extends SessionsClient with LazyLogging {

  private val instance = newClient()
  private val auth: RequestAuthorization = newAuthorization

  private def newClient(requestTimeoutSeconds: Option[Int] = None): Try[SessionsService.MethodPerEndpoint] = {
    for {
      config <- Try(Configuration.load())
      env <- Try(FinagleClient.getEnvironment(config))
      dest <- Try(FinagleClient.newThriftUrl("com.evidence.service.sessions-service", env, "thrift"))
      client <- Try(FinagleClient.newThriftClient().build[SessionsService.MethodPerEndpoint](dest))
    } yield client
  }

  private def newAuthorization = {
    val config = Configuration.load()
    val secret = config.getString("edc.service.sessions.thrift_auth_secret")
    val authType = config.getString("edc.service.sessions.thrift_auth_type")
    RequestAuthorization(Option(authType), Option(secret))
  }

  def getAuthorization(tokenType: SessionTokenType, token: String): Try[Future[GetAuthorizationResponse]] = {
    instance.flatMap {sessions => Try(sessions.getAuthorization(auth, GetAuthorizationRequest(tokenType, token)).toScalaFuture)}
  }

  def createSession(subject: EntityDescriptor,
                    audience: EntityDescriptor,
                    subjectScopes: Set[String],
                    tokenType: SessionTokenType,
                    ttlSeconds: Option[Int],
                    userRoleId: Option[Long],
                    authClient: Option[SessionAuthClient]
                    ): Try[Future[CreateSessionResponse]] = {
    instance.flatMap {sessions => Try(sessions.createSession(auth, CreateSessionRequest(subject, audience, subjectScopes, tokenType, ttlSeconds, userRoleId, authClient)).toScalaFuture)}
  }

  def deleteSession(tokenType: SessionTokenType, token: String): Try[Future[Unit]] = {
    instance.flatMap {sessions => Try(sessions.deleteSession(auth, DeleteSessionRequest(tokenType, token)).toScalaFuture)}
  }
}
