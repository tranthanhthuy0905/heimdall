package utils

import com.evidence.service.common.ServiceGlobal.statsd

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait LatencyHelper {
  def createLatencyMetric(method: Future[URL], metricName: String, startTime: Long, tagList: String*) = {
    method onComplete {
      case Success(_) =>
        val tags = tagList :+ "status:success"
        statsd.time(metricName, System.currentTimeMillis() - startTime, tags: _*)
      case Failure(_) =>
        val tags = tagList :+ "status:fail"
        statsd.time(metricName, System.currentTimeMillis() - startTime, tags: _*)
    }
    method
  }
}
