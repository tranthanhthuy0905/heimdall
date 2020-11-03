package services.rtm

import akka.util.ByteString
import com.evidence.service.common.logging.LazyLogging
import play.api.http.{HttpEntity, Status}
import play.api.libs.ws.WSResponse
import play.api.mvc.{ResponseHeader, Result}

import scala.concurrent.Future

object RtmResponseHandler extends LazyLogging {

  def apply(response: WSResponse, okCallback: WSResponse => Result, logVars: Seq[(String, Any)]): Result = {
    response.status match {
      case Status.OK =>
        okCallback(response)
      case Status.BAD_REQUEST =>
        logger.warn("rtmBadRequest")(logVars: _*)
        toResult(response)
      case Status.TOO_MANY_REQUESTS =>
        logger.warn("tooManyRtmRequests")(logVars: _*)
        toResult(response)
      case _ =>
        logger.info("nonOkRtmResponse")(logVars: _*)
        toResult(response)
    }
  }

  def apply(
    response: WSResponse,
    okCallback: WSResponse => Future[Result],
    logVars: Seq[(String, Any)]): Future[Result] = {
    response.status match {
      case Status.OK =>
        okCallback(response)
      case Status.BAD_REQUEST =>
        logger.info("rtmBadRequest")(logVars: _*)
        Future.successful(toResult(response))
      case Status.TOO_MANY_REQUESTS =>
        logger.info("tooManyRtmRequests")(logVars: _*)
        Future.successful(toResult(response))
      case _ =>
        logger.info("nonOkRtmResponse")(logVars: _*)
        Future.successful(toResult(response))
    }
  }

  private def toResult(response: WSResponse): Result = {
    val contentType = response.contentType
    val httpEntity = response.headers
      .get("Content-Length")
      .map(length => HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), Some(contentType)))
      .getOrElse(HttpEntity.Strict(ByteString(response.body), Some(contentType)))

    Result(ResponseHeader(response.status, response.headers.mapValues(_.head)), httpEntity)
  }
}
