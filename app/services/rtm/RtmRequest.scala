package services.rtm

import java.net.{URL, URLEncoder}

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.zookeeper.ServiceEndpoint

import scala.collection.immutable.Map

class RtmRequest(path: String, endpoint: ServiceEndpoint, query: Map[String, Seq[String]]) {
  override def toString(): String = {
    def encodeValue(seq: Seq[String]): String = seq.foldLeft("") {
      case ("", value) => URLEncoder.encode(value, "UTF-8")
      case (s, value) => s + "," + URLEncoder.encode(value, "UTF-8")
    }
    val encodedQuery = query.foldLeft("") {
      case ("", (key, value)) => URLEncoder.encode(key, "UTF-8") + "=" + encodeValue(value)
      case (s, (key, value)) => s + "&" + URLEncoder.encode(key, "UTF-8") + "=" + encodeValue(value)
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
  def apply(path: String, endpoint: ServiceEndpoint, presignedUrl: URL, query: Map[String, Seq[String]]): String = {
    val request = new RtmRequest(path, endpoint, addToQuery(query, Map("source" -> Seq[String](presignedUrl.toString))))
    request.toString
  }

  private def addToQuery(query: Map[String, Seq[String]], newArg: Map[String, Seq[String]]):  Map[String, Seq[String]] =
    query ++ newArg.map{ case (k,v) => k -> (v ++ query.getOrElse(k,Nil)) }
}
