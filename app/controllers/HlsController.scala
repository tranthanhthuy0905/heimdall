package controllers

import java.util.UUID

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import play.api.http.HttpEntity
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.rtm.RtmClient

import scala.concurrent.ExecutionContext

class HlsController @Inject() (rtm: RtmClient,
                               components: ControllerComponents)
                              (implicit assetsFinder: AssetsFinder, ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging {

  def segment(agencyId: UUID, evidenceId: UUID, fileId: UUID): Action[AnyContent] = Action.async { implicit request =>
    rtm.send("/hls/segment", agencyId, evidenceId, fileId, request.queryString) map { u =>
      processResponse(u)
    }
  }

  private def processResponse(response: WSResponse): Result = {
    if (response.status == 200) {
      val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("application/octet-stream")
      response.headers.get("Content-Length") match {
        case Some(Seq(length)) =>
          Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.toLong), Some(contentType)))
        case _ =>
          Ok.chunked(response.bodyAsSource).as(contentType)
      }
    } else {
      logger.error("unexpectedRtmReturnCode")("status" -> response.status)
      BadGateway
    }
  }

}
