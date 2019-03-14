package filters

import akka.stream.Materializer
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import javax.inject.Inject
import models.auth.{AuthorizationAttr, Authorizer}
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.routing.Router

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AxonAuthFilter @Inject()
(implicit val mat: Materializer, ec: ExecutionContext, authorizer: Authorizer)
  extends Filter with LazyLogging with StrictStatsD {

  // TODO delete /media/test routes from the nonRestrictedRoutes
  final val nonRestrictedRoutes = List(
    "/media/alive",
    "/media/test/create-session",
    "/media/test/get-session",
    "/media/test/get-user",
    "/media/test/get-partner"
  )

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis
    val actionName = getActionName(requestHeader)
    executionTime(actionName) {
      if (isNonRestricted(requestHeader)) {
        executeRequest(startTime, nextFilter, requestHeader, actionName)
      } else {
        authorizer.authorize(requestHeader).flatMap {
          case Right(authData) =>
            executeRequest(startTime, nextFilter, requestHeader.addAttr(AuthorizationAttr.Key, authData), actionName)
          case Left(e) =>
            Future.successful(e)
        }
      }
    }
  }

  private def isNonRestricted(requestHeader: RequestHeader): Boolean = {
    nonRestrictedRoutes.contains(requestHeader.path)
  }

  private def executeRequest(
                              startTime: Long,
                              nextFilter: RequestHeader => Future[Result],
                              requestHeader: RequestHeader,
                              actionName: String): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime
      val tags = s"status:${result.header.status.toString.toLowerCase}"
      logger.debug("requestComplete")(
        "action" -> actionName,
        "durationMs" -> requestTime,
        "status" -> result.header.status,
        "path" -> requestHeader.path,
        "tags" -> tags
      )
      statsd.increment(actionName, tags)
      result
    }
  }

  private def getActionName(requestHeader: RequestHeader): String = {
    val action = Try(requestHeader.attrs(Router.Attrs.HandlerDef)) match {
      case Success(handlerDef) =>
        handlerDef.controller + "." + handlerDef.method
      case Failure(exception) =>
        logger.error("unsupportedRequest")("message" -> exception.getMessage, "path" -> requestHeader.path)
        "unknown.unknown"
    }
    action
  }

}
