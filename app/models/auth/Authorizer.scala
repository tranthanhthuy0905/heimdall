package models.auth

import com.evidence.api.thrift.v1.SessionTokenType
import com.evidence.service.common.auth.jwt.VerifyingJWTParser
import com.evidence.service.common.auth.{CachingJOSEComponentFactory, KeyManager}
import com.evidence.service.common.config.Configuration
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.mvc.{RequestHeader, Result, Results}
import services.sessions.SessionsClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class AuthorizationData(jwt: JWTWrapper)

trait Authorizer {
  protected val config: Config = Configuration.load()
  protected val keyManager: KeyManager = KeyManager.apply(config)
  protected val componentFactory: CachingJOSEComponentFactory = new CachingJOSEComponentFactory(keyManager)
  protected val parser = new VerifyingJWTParser(componentFactory)

  def authorize(requestHeader: RequestHeader): Future[Either[Result, AuthorizationData]]
}

@Singleton
class AuthorizerImpl @Inject()(sessions: SessionsClient)(implicit val executionContext: ExecutionContext)
  extends Authorizer with LazyLogging {

  def authorize(requestHeader: RequestHeader): Future[Either[Result, AuthorizationData]] = {
    getCookieValue(requestHeader) match {
      case Some(t) =>
        logger.debug("found token:" + t.toString)()
        getAuthorizationData(t).map {
          case Left(sessionsError) => Left(sessionsError)
          case Right(authData) => Right(authData)
        }
      case None =>
        logger.error("failedToFindACookie")("message" -> "Could not find Axon Cookie")
        Future.successful(Left(Results.Unauthorized))
    }
  }

  private def getAuthorizationData(token: String): Future[Either[Result, AuthorizationData]] = {
    sessions.getAuthorization(SessionTokenType.SessionCookie, token) match {
      case Success(value) => value.map(u => {
        parser.parse(u.authorization.jwt) match {
          case Right(jwt) => Right(AuthorizationData(JWTWrapper(jwt)))
          case Left(error) =>
            logger.error("failedToParseToken")("token" -> u.authorization.jwt, "error" -> error)
            Left(Results.Unauthorized)
        }
      })
      case Failure(exception) => {
        logger.warn("failedToGetAuthorizationData")("token" -> token, "exception" -> exception.getMessage)
        Future.successful(Left(Results.Unauthorized))
      }
    }
  }

  private def getCookieValue(request: RequestHeader): Option[String] = {
    request.cookies.get("AXONSESSION").map(_.value)
  }

}
