package utils

import com.evidence.service.common.logging.LazyLogging
import models.common.MediaIdent
import play.api.http.HttpEntity
import play.api.libs.ws.WSResponse
import play.api.mvc._

trait HdlResponseHelpers extends BaseController with LazyLogging {

  private def getContentType(response: WSResponse, defaultContentType: String): String = {
    response.headers
      .get("Content-Type")
      .flatMap(_.headOption)
      .getOrElse(defaultContentType)
  }

  def streamedSuccessResponse(response: WSResponse, defaultContentType: String): Result = {
    streamed(response, 200, defaultContentType)
  }

  def streamedResponse(response: WSResponse, defaultContentType: String): Result = {
    streamed(response, response.status, defaultContentType)
  }

  private def streamed(response: WSResponse, status: Int, defaultContentType: String): Result = {
    val contentType = getContentType(response, defaultContentType)
    response.headers
      .get("Content-Length")
      .map(length =>
        new Status(status)
          .sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), Some(contentType))))
      .getOrElse(new Status(status).chunked(response.bodyAsSource).as(contentType))
  }

  def error(errorStatus: Int): Result = Result(ResponseHeader(errorStatus), HttpEntity.NoEntity)

  def errorWithLog(errorStatus: Int)(name: String, logVars: Seq[(String, Any)]): Result = {
    logger.error(name)(logVars: _*)
    error(errorStatus)
  }

  def toHttpStatus(name: String)(error: Throwable, mediaIndent: Option[MediaIdent] = None): Int = {
    logger.error(error, name)(
      "exception"   -> error.getMessage,
      "mediaIndent" -> mediaIndent
    )
    INTERNAL_SERVER_ERROR
  }
}
