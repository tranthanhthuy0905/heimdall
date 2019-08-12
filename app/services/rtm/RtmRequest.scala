package services.rtm

import java.net.{URL, URLEncoder}

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.zookeeper.ServiceEndpoint
import models.common.HeimdallRequest

import scala.collection.immutable.Map

/**
  * RtmRequest generates request URI digestible by RTM.
  *
  * @param path specifies RTM API to call.
  * @param endpoint instance of RTM provided by Zookeeper Server set.
  * @param query RTM request parameters, filtered and validated.
  * @return Generated URI as a string.
  */
class RtmRequest[A](
  path: String,
  endpoint: ServiceEndpoint,
  presignedUrls: List[URL],
  query: Map[String, String],
  request: HeimdallRequest[A])
    extends HeimdallRequest[A](request) {
  override def toString: String = {
    val queryWithSources = query ++ Map("source" -> presignedUrls.mkString(","))
    val encodedQuery = queryWithSources.foldLeft("") {
      case ("", (key, value)) =>
        URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(
          value,
          "UTF-8"
        )
      case (s, (key, value)) =>
        s + "&" + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(
          value,
          "UTF-8"
        )
    }
    Uri
      .from(
        scheme = "https",
        host = endpoint.host,
        port = endpoint.port,
        path = path,
        queryString = Some(encodedQuery)
      )
      .toString
  }
}
