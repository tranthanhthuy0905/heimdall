package controllers

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import play.api.http.HttpEntity
import play.api.mvc._
import services.rtm.{FileIdent, RtmClient}

import scala.concurrent.{ExecutionContext, Future}

case class HlsRequest[A](val file: FileIdent, request: Request[A]) extends WrappedRequest[A](request)

class HlsActionBuilder @Inject()(defaultParser: BodyParsers.Default)
                                (implicit ex: ExecutionContext)
  extends ActionBuilder[HlsRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A],
                              block: HlsRequest[A] => Future[Result]): Future[Result] = {
    // TODO: still add a better request validation here
    val maybeFile = for {
      fileIdSeq <- request.queryString.get("file_id")
      fileId <- fileIdSeq.headOption
      evidenceIdSeq <- request.queryString.get("evidence_id")
      evidenceId <- evidenceIdSeq.headOption
      partnerIdSeq <- request.queryString.get("partner_id")
      partnerId <- partnerIdSeq.headOption
    } yield FileIdent(fileId, evidenceId, partnerId)
    maybeFile.map(file => block(HlsRequest(file, request))).getOrElse(Future.successful(Results.BadRequest))
  }

  override def parser = defaultParser

  override def executionContext = ex
}

class HlsController @Inject()(hlsAction: HlsActionBuilder,
                              rtm: RtmClient,
                              components: ControllerComponents)
                             (implicit assetsFinder: AssetsFinder, ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging {

  def master: Action[AnyContent] = hlsAction.async { implicit request =>
    serveHlsManifest("/hls/master", request)
  }

  def variant: Action[AnyContent] = hlsAction.async { implicit request =>
    serveHlsManifest("/hls/variant", request)
  }

  def segment: Action[AnyContent] = hlsAction.async { implicit request =>
    rtm.send("/hls/segment", request.file, request.queryString) map { response =>
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
    rtm.send(path, request.file, request.queryString) map { response =>
      if (response.status == OK) {
        val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("application/x-mpegURL")
        val newManifest = rewriteManifest(response.body, request.file)
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

  // TODO move HLS related methods to models/hls.
  private def rewriteManifest(manifest: String, fileIdent: FileIdent): String = {
    // TODO make replacement path configurable to match nginx path for QA, Staging and Prod and to be /media/hls for localdev.
    manifest.replaceAllLiterally(
      "/hls/", "/media/hls/"
    ).replaceAll(
      "source=[^&]*&", fileIdent.toString + "&"
    ).replaceAll(
      "source=.*$", fileIdent.toString
    )
  }

}
