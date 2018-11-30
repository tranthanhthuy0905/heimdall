package services.rtm

import java.net.{URL, URLEncoder}

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.zookeeper.ServiceEndpoint

import scala.collection.immutable.Map

// The purpose of RtmRequest is to convert HlsQueryParameters, request path and endpoint (obtained from Zookeeper)
// into a format consumable by RTM.
class RtmRequest(path: String, endpoint: ServiceEndpoint, query: Map[String, String]) {
  override def toString(): String = {
    val encodedQuery = query.foldLeft("") {
      case ("", (key, value)) => URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
      case (s, (key, value)) => s + "&" + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
    }
    Uri.from(scheme = "https",
      host = endpoint.host,
      port = endpoint.port,
      path = path,
      queryString = Some(encodedQuery)
    ).toString
  }
}

object RtmRequest {
  def apply(path: String, endpoint: ServiceEndpoint, presignedUrl: URL, query: Map[String, String]): String = {
    val request = new RtmRequest(path, endpoint, query ++ Map("source" -> presignedUrl.toString))
    request.toString
  }
}
