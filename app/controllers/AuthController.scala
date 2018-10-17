package controllers

import com.evidence.api.thrift.v1.SessionTokenType

import scala.concurrent.ExecutionContext
import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import com.evidence.service.common.logging.LazyLogging
import models.auth.Authorizer

class AuthControllerV0 @Inject()(components: ControllerComponents,
                                 auth: Authorizer)(implicit assetsFinder: AssetsFinder, ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging {

  def get(token: String): Action[AnyContent] = Action.async {
    auth.getAuthorizationData(token).map {
      case Left(e) => InternalServerError(Json.obj("status"-> "500 INTERNAL_SERVER_ERROR", "error" -> e.msg))
      case Right(value) => Ok(Json.obj("status" -> "ok", "response" -> value.jwt.toString, "partner_id" -> value.jwt.audienceId, "scopes" -> value.jwt.scopes))
    }
  }
}
