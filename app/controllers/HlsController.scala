package controllers

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import java.util.UUID

import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._
import com.evidence.service.common.logging.LazyLogging
import services.dredd.DreddClient

class HlsController @Inject() (
                                components: ControllerComponents,
                                dredd: DreddClient) (implicit assetsFinder: AssetsFinder, ex: ExecutionContext)
extends AbstractController(components) with LazyLogging {

  // TODO: change type of arguments from UUID to string. See https://git.taservs.net/ecom/lantern/blob/aeea6396474d575cf8683a0f0798ca3e1aadc593/app/controllers/Evidence.scala#L261
  // TODO FIXME: Prohibit untrusted Http-Request parameter
  def hls(agencyId: UUID, evidenceId: UUID, fileId: UUID): Action[AnyContent] = Action.async {
    dredd.getPresignedUrl2(agencyId, evidenceId, fileId) match {
      case Success(value) => value.map(u => {
        Ok(Json.obj("status" -> "ok", "presignedUrlResponse" -> u.toString))
      })
      case Failure(exception) => {
        logger.error(exception, "Failed to get presigned URL")()
        Future.successful(InternalServerError(Json.obj("status"-> "500 INTERNAL_SERVER_ERROR", "exception" -> exception.getMessage)))
      }
    }
  }
  // TODO: support List[String] instead of just String.
}
