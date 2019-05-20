package services.rtm

import akka.util.ByteString
import com.evidence.service.common.logging.LazyLogging
import play.api.http.{HttpEntity, Status}
import play.api.libs.ws.WSResponse
import play.api.mvc.{ResponseHeader, Result}

object RtmResponseHandler extends LazyLogging {
  def apply(response: WSResponse, okCallback: WSResponse => Result, logVars: Seq[(String, Any)]): Result = {
    response.status match {
      case Status.OK =>
        okCallback(response)
      case Status.BAD_REQUEST =>
        logger.warn("rtmBadRequest")(logVars:_*)
        toResult(response)
      case Status.TOO_MANY_REQUESTS =>
        logger.info("tooManyRtmRequests")(logVars:_*)
        toResult(response)
      case _ =>
        logger.info("nonOkRtmResponse")(logVars:_*)
        toResult(response)
    }
  }

  private def toResult(response: WSResponse): Result = {
    val headers = response.headers.map { case (k,Seq(v)) => (k,v) }
    val entity = HttpEntity.Strict(
      ByteString(response.body.getBytes),
      response.headers.get("Content-Type").map(_.head)
    )
    Result(ResponseHeader(response.status, headers), entity)
  }
}

