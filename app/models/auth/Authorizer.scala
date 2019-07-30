package models.auth

import com.evidence.api.thrift.v1.SessionTokenType
import com.evidence.service.common.auth.jwt.VerifyingJWTParser
import com.evidence.service.common.auth.{CachingJOSEComponentFactory, KeyManager}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.sessions.api.thrift.v1.SessionsServiceErrorCode
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.mvc.{RequestHeader, Result, Results}
import services.sessions.SessionsClient

import scala.concurrent.{ExecutionContext, Future}

case class AuthorizationData(token: String, jwt: String, parsedJwt: JWTWrapper)

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
          case Left(sessionsError) =>
            logger.warn("failedToAuthorizeRequest")(
              "request" -> requestHeader,
              "error" -> sessionsError
            )
            Left(sessionsError)
          case Right(authData) => Right(authData)
        }
      case None =>
        logger.warn("requestWithoutCookie")(
          "message" -> "Cannot fulfill request without Axon Cookie",
          "request" -> requestHeader
        )
        Future.successful(Left(Results.Unauthorized))
    }
  }

  private def getAuthorizationData(token: String): Future[Either[Result, AuthorizationData]] = {
    sessions.getAuthorization(SessionTokenType.SessionCookie, token) map { response =>
      response match {
        case Right(response) => parser.parse(response.authorization.jwt) match {
          case Right(jwt) =>
            Right(AuthorizationData(token, response.authorization.jwt, JWTWrapper(jwt)))
          case Left(error) =>
            logger.info("failedToParseAuthToken")("token" -> response.authorization.jwt, "error" -> error)
            Left(Results.Unauthorized)
        }
        case Left(sessionErrorCode) =>
          sessionErrorCode match {
            case SessionsServiceErrorCode.NotFound => Left(Results.NotFound)
            case _ => Left(Results.InternalServerError)
          }
      }
    }
  }

  private def getCookieValue(request: RequestHeader): Option[String] =
    request.cookies.get("AXONSESSION").map(_.value)

}
