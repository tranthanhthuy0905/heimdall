package controllers

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.zookeeper.ZookeeperServerSet

import scala.util.{Failure, Success}

class ZookeeperControllerV0 @Inject()(
                                components: ControllerComponents,
                                zookeeper: ZookeeperServerSet)(implicit assetsFinder: AssetsFinder)
  extends AbstractController(components) with LazyLogging {

  def getInstance(key: String): Action[AnyContent] = Action {
    zookeeper.getInstance(key) match {
      case Success(value)  => Ok(Json.obj("status" -> "ok", "result" -> value.toString))
      case Failure(exception) => InternalServerError(Json.obj("status"-> "500 INTERNAL_SERVER_ERROR", "exception" -> exception.getMessage))
    }
  }
}
