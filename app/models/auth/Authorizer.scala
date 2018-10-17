package models.auth

import com.evidence.api.thrift.v1.SessionTokenType

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import com.evidence.service.common.auth.jwt.VerifyingJWTParser
import com.evidence.service.common.config.Configuration
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.auth.{CachingJOSEComponentFactory, KeyManager}
import com.typesafe.config.Config
import services.sessions.SessionsClient

import scala.util.{Failure, Success}

case class AuthorizationData(jwt: JWTWrapper)

trait Authorizer {
  protected val config: Config = Configuration.load()
  protected val keyManager: KeyManager = KeyManager.apply(config)
  protected val componentFactory: CachingJOSEComponentFactory = new CachingJOSEComponentFactory(keyManager)
  protected val parser = new VerifyingJWTParser(componentFactory)

  def getAuthorizationData(token: String): Future[Either[AuthorizationError, AuthorizationData]]
}

@Singleton
class AuthorizerImpl @Inject()(sessions: SessionsClient)(implicit val executionContext: ExecutionContext)
  extends Authorizer with LazyLogging {

  def getAuthorizationData(token: String): Future[Either[AuthorizationError, AuthorizationData]] = {
    sessions.getAuthorization(SessionTokenType.AuthToken, token) match {
      case Success(value) => value.map(u => {
        parser.parse(u.authorization.jwt) match {
          case Right(jwt) => Right(AuthorizationData(JWTWrapper(jwt)))
          case Left(error) =>
            logger.warn("tokenParseFailed")("token" -> u.authorization.jwt, "error" -> error)
            Left(TokenParseError("Header authorization failed, bad token"))
        }
      })
      case Failure(exception) => {
        logger.error(exception, "Failed to get authorization")("token" -> token)
        val message = exception.getMessage
        Future.successful(Left(BackendServiceError(s"Header authorization failed error:$message")))
      }
    }
  }
}
