package controllers

import java.util.UUID

import com.evidence.api.thrift.v1.{EntityDescriptor, SessionAuthClient, SessionTokenType, TidEntities}
import com.evidence.service.common.auth.jwt.ECOMScopes
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.dredd.DreddClient
import services.sessions.SessionsClient
import services.zookeeper.ZookeeperServerSet

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

// TODO Delete this controller and test routes after heimdall has sufficient unit tests and before launch in prod.
// TODO XXX Ignore codacy warnings related to this controller.
class TemporaryController @Inject() (
                                       components: ControllerComponents,
                                       sessions: SessionsClient,
                                       zookeeper: ZookeeperServerSet,
                                       dredd: DreddClient) (implicit assetsFinder: AssetsFinder, ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging {

  def getSession: Action[AnyContent] = Action.async { implicit request =>
    val token = request.queryString("token").head //XXX: no need to check because this is a temp test controller.
    sessions.getAuthorization(SessionTokenType.AuthToken, token) match {
      case Success(value) => value.map(u => {
        Ok(Json.obj("status" -> "ok", "response" -> u.authorization.toString()))
      })
      case Failure(exception) => {
        logger.error(exception, "Failed to get authorization by token")()
        Future.successful(InternalServerError(Json.obj("status"-> "500 INTERNAL_SERVER_ERROR", "exception" -> exception.getMessage)))
      }
    }
  }

  def createSession: Action[AnyContent] = Action.async { implicit request =>
    val agencyId = request.queryString("agency").head //XXX: no need to check because this is a temp test controller.
    val entity = EntityDescriptor(TidEntities.Subscriber, UUID.randomUUID().toString.toUpperCase, Option(agencyId))
    val domain = EntityDescriptor(TidEntities.Partner, agencyId.toString, Option(agencyId))
    val scopes = Set[String](ECOMScopes.EVIDENCE_ANY_LIST, ECOMScopes.CASES_ANY_MODIFY) // TODO this method will be removed, it is needed for testing only
    val tokenType = SessionTokenType.AuthToken
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

  def getRtmInstance(key: String): Action[AnyContent] = Action {
    zookeeper.getInstance(key) match {
      case Success(value)  => Ok(Json.obj("status" -> "ok", "result" -> value.toString))
      case Failure(exception) => InternalServerError(Json.obj("status"-> "500 INTERNAL_SERVER_ERROR", "exception" -> exception.getMessage))
    }
  }

  def getUrl(agencyId: UUID, evidenceId: UUID, fileId: UUID): Action[AnyContent] = Action.async {
    dredd.getUrl(agencyId, evidenceId, fileId) map { u =>
      Ok(Json.obj("status" -> "ok", "presignedUrlResponse" -> u.toString))
    }
  }

}
