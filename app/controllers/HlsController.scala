package controllers

import com.typesafe.config.Config
import javax.inject.Inject
import models.auth.AuthorizationAttr
import models.hls.{HeimdallHlsActionBuilder, HlsManifestFormatter, Watermark}
import play.api.http.HttpEntity
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.nino.{NinoClient, NinoClientAction}
import services.rtm.{RtmClient, RtmResponseHandler}

import scala.concurrent.ExecutionContext

class HlsController @Inject()(action: HeimdallHlsActionBuilder,
                              rtm: RtmClient,
                              watermark: Watermark,
                              nino: NinoClient,
                              config: Config,
                              components: ControllerComponents)
                             (implicit ex: ExecutionContext)
  extends AbstractController(components) {

  def playlist: Action[AnyContent] = action.async { implicit request =>
    val authHandler = request.attrs(AuthorizationAttr.Key)
    val rtmResponse = for {
      // TODO: refactor actions and move the access logic into a separate action builder.
      // TODO: this can be done after performance requirements are determined and met.
      accessResult <- nino.enforce(authHandler.jwtString, request.rtmQuery.media.toFileEntityDescriptors, NinoClientAction.Stream)
      _ <- utils.Predicate(accessResult)(new Exception(
        s"${request.path}: media [${request.rtmQuery.media}] does not have ${NinoClientAction.Stream} access"
      ))
      response <- rtm.send(request.rtmQuery)
    } yield response

    val okCallback = (response: WSResponse) => {
      val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("application/x-mpegURL")
      val newManifest =
        HlsManifestFormatter(
          response.body,
          request.rtmQuery.media,
          config.getString("heimdall.api_prefix"),
          request.streamingSessionToken
        )
      Ok(newManifest).as(contentType)
    }

    rtmResponse map { response =>
      RtmResponseHandler(response, okCallback,  Seq[(String, Any)](
        "path" -> request.rtmQuery.path,
        "token"-> request.streamingSessionToken,
        "media" -> request.rtmQuery.media,
        "status" -> response.status,
        "message" -> response.body
      ))
    }
  }

  def segment: Action[AnyContent] = action.async { implicit request =>
    val authHandler = request.attrs(AuthorizationAttr.Key)
    val rtmResponse = for {
      // TODO: refactor actions and move the access logic into a separate action builder.
      // TODO: this can be done after performance requirements are determined and met.
      accessResult <- nino.enforce(authHandler.jwtString, request.rtmQuery.media.toFileEntityDescriptors, NinoClientAction.Stream)
      _ <- utils.Predicate(accessResult)(new Exception(s"${request.path}: media [${request.rtmQuery.media}] does not have ${NinoClientAction.Stream} access"))
      queryWithWatermark <- watermark.augmentQuery(request.rtmQuery, authHandler.jwt)
      response <- rtm.send(queryWithWatermark)
    } yield response

    val okCallback = (response: WSResponse) => {
      val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("video/MP2T")
      response.headers.get("Content-Length") match {
        case Some(Seq(length)) =>
          Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.toLong), Some(contentType)))
        case _ =>
          Ok.chunked(response.bodyAsSource).as(contentType)
      }
    }

    rtmResponse map { response =>
      RtmResponseHandler(response, okCallback, Seq[(String, Any)](
        "path" -> request.rtmQuery.path,
        "token"-> request.streamingSessionToken,
        "media" -> request.rtmQuery.media,
        "status" -> response.status,
        "message" -> response.body
      ))
    }
  }

}
