package controllers.v1

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction, TokenValidationAction}
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common.PermissionType
import play.api.http.HttpEntity
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.rtm.RtmClient
import utils.WSResponseHelpers

import scala.concurrent.ExecutionContext

class AudioController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with WSResponseHelpers {

  def sample: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        tokenValidationAction andThen
        permValidation.build(PermissionType.View) andThen
        rtmRequestAction
    ).async { request =>
      FutureEither(rtm.send(request).map(withOKStatus))
        .map(toSample)
        .fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), r => r)
    }

  def mp3: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        rtmRequestAction
    ).async { implicit request =>
      FutureEither(rtm.send(request).map(withOKStatus))
        .map(toResult)
        .fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), r => r)
    }

  private def toSample(response: WSResponse): Result = {
    val contentType = response.headers
      .get("Content-Type")
      .flatMap(_.headOption)
      .getOrElse("application/json")
    Ok(response.json).as(contentType)
  }

  private def toResult(response: WSResponse): Result = {
    val contentType = response.headers
      .get("Content-Type")
      .flatMap(_.headOption)
      .getOrElse("audio/mpeg")
    response.headers
      .get("Content-Length")
      .map(length =>
        Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), Some(contentType))))
      .getOrElse(Ok.chunked(response.bodyAsSource).as(contentType))
  }
}
