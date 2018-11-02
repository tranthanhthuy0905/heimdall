
import akka.stream.Materializer
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.auth.{AuthorizationData, Authorizer}
import play.api.libs.typedmap.TypedKey
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.routing.Router

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AxonAuthFilter @Inject()
  (implicit val mat: Materializer, ec: ExecutionContext, authorizer: Authorizer)
  extends Filter with LazyLogging {

  final val nonRestrictedRoutes = List("/media/alive", "/media/v0/sessions", "/media/v0/zookeeper")

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis
    if (isAuthRequired(requestHeader)) {
      authorizer.authorize(requestHeader).flatMap {
        case Right(a) =>
          val requestHeaderWithAuth = requestHeader.addAttr(TypedKey.apply[AuthorizationData]("auth"), a)
          executeRequest(startTime, nextFilter, requestHeaderWithAuth)
        case Left(e) =>
          Future.successful(e)
      }
    } else {
      executeRequest(startTime, nextFilter, requestHeader)
    }
  }

  private def isAuthRequired(requestHeader: RequestHeader) : Boolean = {
    // TODO when ready to prod-launch Heimdall, update the method with `nonRestrictedRoutes.contains(requestHeader.path)`
    val foundNonRestrictedRoute = for (nonRestrictedRoute <- nonRestrictedRoutes if requestHeader.path.startsWith (nonRestrictedRoute) )
      yield nonRestrictedRoute
    foundNonRestrictedRoute.isEmpty
  }

  private def executeRequest(startTime: Long, nextFilter: RequestHeader => Future[Result], requestHeader: RequestHeader) : Future[Result] = {
    nextFilter(requestHeader).map { result =>
      val action = Try(requestHeader.attrs(Router.Attrs.HandlerDef)) match {
        case Success(handlerDef) =>
          handlerDef.controller + "." + handlerDef.method
        case Failure(exception) =>
          logger.error("unsupportedRequest")("message"->exception.getMessage, "path" -> requestHeader.path)
          "unknown.unknown"
      }
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime
      logger.info("requestComplete")("action" -> action, "durationMs" -> requestTime, "status" -> result.header.status)
      result
    }
  }

}
