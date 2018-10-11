package controllers

import java.util.UUID

import com.evidence.api.thrift.v1.{EntityDescriptor, SessionAuthClient, SessionTokenType, TidEntities}
import javax.inject._
import play.api.mvc._
import com.evidence.service.common.logging.LazyLogging
import play.api.libs.json.Json
import services.sessions.SessionsClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class SessionsControllerV0 @Inject() (
                                components: ControllerComponents,
                                sessions: SessionsClient) (implicit assetsFinder: AssetsFinder, ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging {

  // TODO FIXME The following codacy warning will go away as the client will be closer to the prod level state:
  // FIXME "The method takes a value that is controlled by the client"
  def get(intTokenType: Int, token: String): Action[AnyContent] = Action.async {
    sessions.getAuthorization(SessionTokenType(intTokenType), token) match {
      case Success(value) => value.map(u => {
        Ok(Json.obj("status" -> "ok", "response" -> u.authorization.toString()))
      })
      case Failure(exception) => {
        logger.error(exception, "Failed to get authorization by token")()
        Future.successful(InternalServerError(Json.obj("status"-> "500 INTERNAL_SERVER_ERROR", "exception" -> exception.getMessage)))
      }
    }
  }

  // TODO FIXME The following codacy warning will go away as the client will be closer to the prod level state:
  // FIXME "The method takes a value that is controlled by the client"
  def create(intTokenType: Int, agencyId: UUID): Action[AnyContent] = Action.async {
    val entity = EntityDescriptor(TidEntities.Subscriber, UUID.randomUUID().toString.toUpperCase, Option(agencyId.toString))
    val domain = EntityDescriptor(TidEntities.Partner, agencyId.toString, Option(agencyId.toString))
    val scopes = Set[String]("itestscopes") // TODO this method will be removed, it is needed for testing only
    val tokenType = SessionTokenType(intTokenType)
    val ttlSeconds = Option(900)
    val userRoleId = Option(10L)
    val authClient = Option(SessionAuthClient.Web)

    sessions.createSession(entity, domain, scopes, tokenType, ttlSeconds, userRoleId, authClient) match {
      case Success(value) => value.map(response => {
        Ok(Json.obj("status" -> "ok", "response" -> response.toString()))
      })
      case Failure(exception) => {
        logger.error(exception, "Failed to create a new sessions")()
        Future.successful(InternalServerError(Json.obj("status"-> "500 INTERNAL_SERVER_ERROR", "exception" -> exception.getMessage)))
      }
    }
  }

  // TODO FIXME The following codacy warning will go away as the client will be closer to the prod level state:
  // FIXME "The method takes a value that is controlled by the client"
  def delete(intTokenType: Int, token: String): Action[AnyContent] = Action.async {
    sessions.deleteSession(SessionTokenType(intTokenType), token) match {
      case Success(value) => value.map(_ => {
        Ok(Json.obj("status" -> "ok"))
      })
      case Failure(exception) => {
        logger.error(exception, "Failed to delete session token")()
        Future.successful(InternalServerError(Json.obj("status"-> "500 INTERNAL_SERVER_ERROR", "exception" -> exception.getMessage)))
      }
    }
  }

}
