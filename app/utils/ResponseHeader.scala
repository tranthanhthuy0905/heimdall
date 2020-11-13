package utils

import play.api.libs.ws.WSResponse
import play.api.mvc.BaseController

trait WSResponseHelpers extends BaseController {

  protected val blacklist = Seq("content-length", "content-type", "transfer-encoding")

  private def byBlackList(key: String): Boolean = blacklist.contains(key.toLowerCase)

  def withHeader(headers: Map[String, Seq[String]]): Map[String, String] = {
    headers
      .filterKeys(!byBlackList(_))
      .mapValues(_.headOption.getOrElse(""))
  }

  def withOKStatus(response: WSResponse): Either[Int, WSResponse] = {
    Some(response)
      .filter(_.status equals OK)
      .toRight(response.status)
  }

}
