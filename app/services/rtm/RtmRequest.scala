package services.rtm

import java.net.{URL, URLEncoder}

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.zookeeper.ServiceEndpoint

import scala.collection.immutable.Map

/**
  * List of RTM request API-s.
  *
  * The enum has to be in sync with RTM's registered paths.
  * See https://git.taservs.net/ecom/rtm/blob/cae8381f91b668eaed143cfe07dd6fa4a8acf6ba/src/rtm/server/http/server.go#L220
  *
  * Note: /flv was deprecated. Thus, Heimdall does not support it.
  */
trait RtmRequestRoutes {
  protected final val Health = "/health"
  protected final val Probe = "/probe"
  protected final val HlsMaster = "/hls/master"
  protected final val HlsVariant = "/hls/variant"
  protected final val HlsSegment = "/hls/segment"
  protected final val Thumbnail = "/thumbnail"
  protected final val Mp3 = "/mp3"
}

/**
  * RtmRequest generates request URI digestible by RTM.
  * @param path specifies RTM API to call.
  * @param endpoint instance of RTM provided by Zookeeper Server set.
  * @param query RTM request parameters, filtered and validated.
  * @return Generated URI as a string.
  */
class RtmRequest(path: String, endpoint: ServiceEndpoint, query: Map[String, String]) {
  override def toString: String = {
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
