package controllers.v1

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction, WatermarkAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common.PermissionType
import play.api.http.HttpEntity
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.rtm.RtmClient
import utils.WSResponseHelpers

import scala.concurrent.ExecutionContext

class ThumbnailController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  watermarkAction: WatermarkAction,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with WSResponseHelpers {

  def thumbnail: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        watermarkAction andThen
        rtmRequestAction
    ).async { request =>
      (
        for {
          response <- FutureEither(rtm.send(request).map(withOKStatus))
        } yield toResult(response)
      ).fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), r => r)
    }

  private def toResult(response: WSResponse): Result = {
    val contentType = response.headers.getOrElse("Content-Type", Seq()).headOption.getOrElse("image/jpeg")
    response.headers
      .get("Content-Length")
      .map(length =>
        Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), Some(contentType))))
      .getOrElse(Ok.chunked(response.bodyAsSource).as(contentType))
  }
}
