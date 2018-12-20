package models.auth

import com.evidence.api.thrift.v1.SessionTokenType
import com.evidence.service.common.auth.jwt.VerifyingJWTParser
import com.evidence.service.common.auth.{CachingJOSEComponentFactory, KeyManager}
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{RequestHeader, Result, Results}
import services.sessions.SessionsClient

import scala.concurrent.{ExecutionContext, Future}

case class AuthorizationData(jwt: JWTWrapper, token: String)

object AuthorizationAttr {
  val Key: TypedKey[AuthorizationData] = TypedKey.apply[AuthorizationData]("auth")
}

trait Authorizer {
  def authorize(requestHeader: RequestHeader): Future[Either[Result, AuthorizationData]]
}

@Singleton
class AuthorizerImpl @Inject()(sessions: SessionsClient, config: Config)(implicit val executionContext: ExecutionContext)
  extends Authorizer with LazyLogging {
  protected val keyManager: KeyManager = KeyManager.apply(config)
  protected val componentFactory: CachingJOSEComponentFactory = new CachingJOSEComponentFactory(keyManager)
  protected val parser = new VerifyingJWTParser(componentFactory)

  def authorize(requestHeader: RequestHeader): Future[Either[Result, AuthorizationData]] = {
    getCookieValue(requestHeader) match {
      case Some(t) =>
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
    sessions.getAuthorization(SessionTokenType.SessionCookie, token) map { u =>
      parser.parse(u.authorization.jwt) match {
        case Right(jwt) => Right(AuthorizationData(JWTWrapper(jwt), token))
        case Left(error) =>
          logger.error("failedToParseAuthToken")("token" -> u.authorization.jwt, "error" -> error)
          Left(Results.Unauthorized)
      }
    }
  }

  private def getCookieValue(request: RequestHeader): Option[String] =
    request.cookies.get("AXONSESSION").map(_.value)

}
