package controllers

import java.util.UUID
import javax.inject._
import play.api.mvc._
import com.evidence.service.common.logging.LazyLogging
import play.api.libs.json.Json
import services.dredd.DreddClient
import scala.concurrent.ExecutionContext

class HlsController @Inject() (
                                components: ControllerComponents,
                                dredd: DreddClient) (implicit assetsFinder: AssetsFinder, ex: ExecutionContext)
extends AbstractController(components) with LazyLogging {

  // TODO: change type of arguments from UUID to string. See https://git.taservs.net/ecom/lantern/blob/aeea6396474d575cf8683a0f0798ca3e1aadc593/app/controllers/Evidence.scala#L261
  // TODO FIXME: Prohibit untrusted Http-Request parameter
  def hls(agencyId: UUID, evidenceId: UUID, fileId: UUID): Action[AnyContent] = Action.async {
    val u = for {
      response <- dredd.getPresignedUrl2(agencyId, evidenceId, fileId)
    } yield response.presignedUrl
    u.map(maybeFile => {
      Ok(Json.obj("status" -> "ok", "presignedUrl" -> maybeFile.toString))
    })
  }
  // TODO: support List[String] instead of just String.
}
