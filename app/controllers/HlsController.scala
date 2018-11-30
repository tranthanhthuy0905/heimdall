package controllers

import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.{QueryValidator, ValidatedQuery}
import models.hls.HlsManifestFormatter
import play.api.http.HttpEntity
import play.api.mvc._
import services.rtm.RtmClient

import scala.concurrent.{ExecutionContext, Future}

case class HlsRequest[A](val validatedQuery: ValidatedQuery, request: Request[A]) extends WrappedRequest[A](request)

class HlsActionBuilder @Inject()(defaultParser: BodyParsers.Default)
                                (implicit ex: ExecutionContext)
  extends ActionBuilder[HlsRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A],
                              block: HlsRequest[A] => Future[Result]): Future[Result] = {
    QueryValidator(request.path, request.queryString).map(
      validatedQuery => block(HlsRequest(validatedQuery, request))
    ).getOrElse(Future.successful(Results.BadRequest))
  }

  override def parser = defaultParser

  override def executionContext = ex
}

class HlsController @Inject()(hlsAction: HlsActionBuilder,
                              rtm: RtmClient,
                              config: Config,
                              components: ControllerComponents)
                             (implicit ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging {

  def master: Action[AnyContent] = hlsAction.async { implicit request =>
    serveHlsManifest("/hls/master", request)
  }

  def variant: Action[AnyContent] = hlsAction.async { implicit request =>
    serveHlsManifest("/hls/variant", request)
  }

  def segment: Action[AnyContent] = hlsAction.async { implicit request =>
    rtm.send("/hls/segment", request.validatedQuery) map { response =>
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

  private def serveHlsManifest[A](path: String, request: HlsRequest[A]): Future[Result] = {
    rtm.send(path, request.validatedQuery) map { response =>
      if (response.status == OK) {
        val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("application/x-mpegURL")
        val newManifest = HlsManifestFormatter(response.body, request.validatedQuery.file, config.getString("heimdall.api_prefix"))
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
