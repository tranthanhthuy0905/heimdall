package utils

import com.evidence.service.common.logging.LazyLogging
import play.api.mvc.RequestHeader

object RequestUtils extends LazyLogging {

  /**
    * Gets the client IP address using remoteAddress API.
    * See https://www.playframework.com/documentation/2.6.0/api/java/play/mvc/Http.RequestHeader.html#remoteAddress--
    *
    * @param request HTTP Request.
    * @return IP Address as a string.
    */
  def getClientIpAddress(request: RequestHeader): String = {
    request.remoteAddress
  }
}
