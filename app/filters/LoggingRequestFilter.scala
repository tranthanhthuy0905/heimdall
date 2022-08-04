package filters

import akka.stream.Materializer
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import javax.inject.Inject
import models.auth.Authorizer
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.routing.Router
import java.time.Duration
import java.lang.System

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class LoggingRequestFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext, authorizer: Authorizer)
  extends Filter
    with LazyLogging
    with StrictStatsD {

  private val logInterval = Duration.ofSeconds(6)

  def apply(
             nextFilter: RequestHeader => Future[Result]
           )(requestHeader: RequestHeader): Future[Result] = {
    val startTime          = System.currentTimeMillis
    val actionName: String = this.getActionName(requestHeader)

    executionTime(
      s"$actionName.time",
      executeRequest(
        startTime,
        System.currentTimeMillis,
        nextFilter,
        requestHeader,
        actionName
      ),
      includeServiceLevelStat = true)
  }

  private def executeRequest(
                              startTime: Long,
                              executeStartTime: Long,
                              nextFilter: RequestHeader => Future[Result],
                              requestHeader: RequestHeader,
                              actionName: String): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      val requestTime = System.currentTimeMillis - startTime
      if (requestTime > logInterval.toMillis) {
        logger.warn("requestDelay")(
          "action"   -> actionName,
          "path"     -> requestHeader.path,
          "query"    -> requestHeader.queryString,
          "duration" -> s"${requestTime}ms",
          "overhead" -> s"${executeStartTime - startTime}ms",
          "status"   -> result.header.status,
        )
      }
      statsd.increment(
        s"$actionName.requests",
        s"status:${result.header.status.toString.toLowerCase}"
      )
      result
    }
  }

  private def getActionName(requestHeader: RequestHeader): String = {
    val action = Try(requestHeader.attrs(Router.Attrs.HandlerDef)) match {
      case Success(handlerDef) =>
        handlerDef.controller + "." + handlerDef.method
      case Failure(exception) =>
        logger.error("unsupportedRequest")(
          "message" -> exception.getMessage,
          "path"    -> requestHeader.path
        )
        "unknown.unknown"
    }
    action
  }
}
