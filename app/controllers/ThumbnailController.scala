package controllers

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.auth.AuthorizationAttr
import models.common.HeimdallActionBuilder
import play.api.mvc._
import services.nino.{NinoClient, NinoClientAction}
import services.rtm.RtmClient

import scala.concurrent.ExecutionContext

class ThumbnailController @Inject()(action: HeimdallActionBuilder,
                                    rtm: RtmClient,
                                    nino: NinoClient,
                                    components: ControllerComponents)
                                   (implicit ex: ExecutionContext)
  extends AbstractController(components)
    with LazyLogging {

  def thumbnail: Action[AnyContent] = action.async { implicit request =>
    val authHandler = request.attrs(AuthorizationAttr.Key)
    val rtmResponse = for {
      // TODO: refactor actions and move the access logic into a separate action builder.
      // TODO: this can be done after performance requirements are determined and met.
      accessResult <- nino.enforce(authHandler.jwtString, request.rtmQuery.media, NinoClientAction.View)
      _ <- utils.Predicate(accessResult)(
        new Exception(s"${request.path}: media [${request.rtmQuery.media}] does not have ${NinoClientAction.View} access")
      )
      response <- rtm.send(request.rtmQuery)
    } yield response

    rtmResponse map { response =>
      if (response.status == OK) {
        val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("image/jpeg")
        Ok.chunked(response.bodyAsSource).as(contentType)
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
