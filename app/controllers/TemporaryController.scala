package controllers

import java.util.UUID

import com.evidence.api.thrift.v1.{EntityDescriptor, SessionAuthClient, SessionTokenType, TidEntities}
import com.evidence.service.common.auth.jwt.ECOMScopes
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.dredd.DreddClient
import services.komrade.KomradeClient
import services.sessions.SessionsClient
import services.zookeeper.ZookeeperServerSet

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

// TODO Delete this controller and test routes after heimdall has sufficient unit tests and before launch in prod.
// TODO XXX Ignore codacy warnings related to this controller.
class TemporaryController @Inject()( components: ControllerComponents,
                                     sessions: SessionsClient,
                                     zookeeper: ZookeeperServerSet,
                                     dredd: DreddClient,
                                     komrade: KomradeClient
                                   )(implicit assetsFinder: AssetsFinder, ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging {

  def throwException: Action[AnyContent] = Action.async {
    throw new Exception("HELLO!!!")
    Future.successful(Ok(Json.obj("status" -> "ok")))
  }

  def getUser: Action[AnyContent] = Action.async {
    komrade.getUser("f3d719bc-db2b-4b71-bfb1-436240fb9099","bda513fe-cfb9-4acb-bb25-a665387c12bd") map { u =>
      Ok(Json.obj("status" -> "ok", "response" -> u.toString()))
    }
  }

  def getPartner: Action[AnyContent] = Action.async {
    komrade.getPartner("f3d719bc-db2b-4b71-bfb1-436240fb9099") map { u =>
      Ok(Json.obj("status" -> "ok", "response" -> u.toString()))
    }
  }

  def getSession: Action[AnyContent] = Action.async { implicit request =>
    val token = request.queryString("token").head //XXX: no need to check because this is a temp test controller.
    sessions.getAuthorization(SessionTokenType.SessionCookie, token) map { u =>
      Ok(Json.obj("status" -> "ok", "response" -> u.authorization.toString()))
    }
  }

  def createSession: Action[AnyContent] = Action.async { implicit request =>
    //XXX: no need to check because this is a temp test controller.
    val partnerId = request.queryString("partner").head
    val userId = request.queryString("user").headOption match {
      case Some(id) => id
      case None => "bda513fe-cfb9-4acb-bb25-a665387c12bd"
    }
    val entity = EntityDescriptor(
      TidEntities.Subscriber,
      userId,
      Option(partnerId)
    )
    val domain = EntityDescriptor(TidEntities.Partner, partnerId.toString, Option(partnerId))
    // TODO this method will be removed, it is needed for testing only
    val scopes = Set[String](ECOMScopes.EVIDENCE_ANY_LIST, ECOMScopes.CASES_ANY_MODIFY)
    val tokenType = SessionTokenType.SessionCookie
    val ttlSeconds = Option(900)
    val userRoleId = Option(10L)
    val authClient = Option(SessionAuthClient.Web)
    sessions.createSession(entity, domain, scopes, tokenType, ttlSeconds, userRoleId, authClient) map { response =>
      Ok(Json.obj("status" -> "ok", "response" -> response.toString()))
    }
  }

  def getRtmInstance(key: String): Action[AnyContent] = Action {
    zookeeper.getInstance(key) match {
      case Success(value) => Ok(Json.obj("status" -> "ok", "result" -> value.toString))
      case Failure(exception) => InternalServerError(Json.obj("status" -> "500 INTERNAL_SERVER_ERROR", "exception" -> exception.getMessage))
    }
  }

  def getUrl(agencyId: UUID, evidenceId: UUID, fileId: UUID): Action[AnyContent] = Action.async {
    dredd.getUrl(agencyId, evidenceId, fileId) map { u =>
      Ok(Json.obj("status" -> "ok", "presignedUrlResponse" -> u.toString))
    }
  }

}
