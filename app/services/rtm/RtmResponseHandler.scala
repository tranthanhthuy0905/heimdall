package services.rtm

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
        logger.info("tooManyRtmRequests")(logVars: _*)
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
        logger.warn("rtmBadRequest")(logVars: _*)
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
    Result(
      ResponseHeader(response.status, response.headers.mapValues(_.head)),
      HttpEntity.Strict(
        response.bodyAsBytes,
        Some(response.contentType)
      )
    )
  }
}
