package controllers.v1

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction, TokenValidationAction}
import javax.inject.Inject
import models.common.PermissionType
import play.api.http.HttpEntity
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.rtm.{RtmClient, RtmResponseHandler}

import scala.concurrent.{ExecutionContext, Future}

class AudioController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components) {

  def sample: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        tokenValidationAction andThen
        permValidation.build(PermissionType.View) andThen
        rtmRequestAction
    ).async { request =>
      val okCallback = (response: WSResponse) => {
        val contentType = response.headers
          .get("Content-Type")
          .flatMap(_.headOption)
          .getOrElse("application/json")
        Ok(response.json).as(contentType)
      }

      rtm.send(request) map { response =>
        RtmResponseHandler(
          response,
          okCallback,
          Seq[(String, Any)](
            "path"       -> request.path,
            "mediaIdent" -> request.media,
            "status"     -> response.status,
            "message"    -> response.body
          )
        )
      }
    }

  def mp3: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        rtmRequestAction
    ).async { implicit request =>
      for {
        response   <- rtm.stream(request.toString)
        httpResult <- Future.successful(toResult(response))
      } yield httpResult.fold(BadRequest(_), r => r)
    }

  private def toResult(response: WSResponse): Either[JsObject, Result] = {
    Some(response.status)
      .filter(_ equals play.api.http.Status.OK)
      .toRight(Json.obj("message" -> response.body))
      .map(_ => {
        val contentType = response.headers
          .get("Content-Type")
          .flatMap(_.headOption)
          .getOrElse("audio/mpeg")

        response.headers
          .get("Content-Length")
          .map(length =>
            Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), Some(contentType))))
          .getOrElse(Ok.chunked(response.bodyAsSource).as(contentType))
      })
  }
}
