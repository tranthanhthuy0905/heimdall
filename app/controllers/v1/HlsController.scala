package controllers.v1

import actions._
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common.{HeimdallRequest, PermissionType}
import models.hls.HlsManifestFormatter
import play.api.http.HttpEntity
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.rtm.RtmClient
import utils.WSResponseHelpers

import scala.concurrent.ExecutionContext

class HlsController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  watermarkAction: WatermarkAction,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with WSResponseHelpers {

  def playlist: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        tokenValidationAction andThen
        permValidation.build(PermissionType.Stream) andThen
        rtmRequestAction
    ).async { request =>
      FutureEither(rtm.send(request).map(withOKStatus))
        .map(toManifest(_, request))
        .fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), r => r)
    }

  def segment: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        tokenValidationAction andThen
        permValidation.build(PermissionType.Stream) andThen
        watermarkAction andThen
        rtmRequestAction
    ).async { request =>
      FutureEither(rtm.send(request).map(withOKStatus))
        .map(toResult)
        .fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), r => r)
    }

  private def toResult(response: WSResponse): Result = {
    val contentType = response.headers.getOrElse("Content-Type", Seq()).headOption.getOrElse("video/MP2T")
    response.headers
      .get("Content-Length")
      .map(length =>
        Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), Some(contentType))))
      .getOrElse(Ok.chunked(response.bodyAsSource).as(contentType))
  }

  private def toManifest(response: WSResponse, request: HeimdallRequest[AnyContent]): Result = {
    val contentType = response.headers
      .get("Content-Type")
      .flatMap(_.headOption)
      .getOrElse("application/x-mpegURL")
    val newManifest =
      HlsManifestFormatter(
        response.body,
        request.media,
        request.apiPathPrefixForBuildingHlsManifest,
        Some(request.streamingSessionToken)
      )
    Ok(newManifest).as(contentType)
  }
}
