package controllers

import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.hls.HlsManifestFormatter
import models.play.{HeimdallActionBuilder, HeimdallRequest}
import play.api.http.HttpEntity
import play.api.mvc._
import services.rtm.RtmClient

import scala.concurrent.{ExecutionContext, Future}

class HlsController @Inject()(action: HeimdallActionBuilder,
                              rtm: RtmClient,
                              config: Config,
                              components: ControllerComponents)
                             (implicit ex: ExecutionContext)
  extends AbstractController(components)
    with LazyLogging {

  def master: Action[AnyContent] = action.async { implicit request =>
    serveHlsManifest(request.rtmQuery.path, request)
  }

  def variant: Action[AnyContent] = action.async { implicit request =>
    serveHlsManifest(request.rtmQuery.path, request)
  }

  def segment: Action[AnyContent] = action.async { implicit request =>
    rtm.send(request.rtmQuery) map { response =>
      if (response.status == OK) {
        val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("video/MP2T")
        response.headers.get("Content-Length") match {
          case Some(Seq(length)) =>
            Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.toLong), Some(contentType)))
          case _ =>
            Ok.chunked(response.bodyAsSource).as(contentType)
        }
      } else {
        logger.error(s"unexpectedHlsSegmentReturnCode")("status" -> response.status)
        InternalServerError
      }
    }
  }

  private def serveHlsManifest[A](path: String, request:  HeimdallRequest[A]): Future[Result] = {
    rtm.send(request.rtmQuery) map { response =>
      if (response.status == OK) {
        val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("application/x-mpegURL")
        val newManifest = HlsManifestFormatter(response.body, request.rtmQuery.file, config.getString("heimdall.api_prefix"))
        Ok(newManifest).as(contentType)
      } else {
        logger.error(s"unexpectedHlsManifestReturnCode")(
          "status" -> response.status,
          "message" -> response.body
        )
        InternalServerError
      }
    }
  }

}
