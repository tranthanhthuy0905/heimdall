package controllers

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.play.HeimdallActionBuilder
import play.api.mvc._
import services.rtm.RtmClient

import scala.concurrent.ExecutionContext

class ThumbnailController @Inject()(action: HeimdallActionBuilder,
                                    rtm: RtmClient,
                                    components: ControllerComponents)
                                   (implicit ex: ExecutionContext)
  extends AbstractController(components)
    with LazyLogging {

  def thumbnail: Action[AnyContent] = action.async { implicit request =>
    rtm.send(request.rtmQuery) map { response =>
      if (response.status == OK) {
        val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("image/jpeg")
        Ok(response.bodyAsBytes).as(contentType)
      } else {
        logger.error("unexpectedThumbnailReturnCode")(
          "path" -> request.rtmQuery.path,
          "status" -> response.status,
          "message" -> response.body
        )
        InternalServerError
      }
    }
  }

}
