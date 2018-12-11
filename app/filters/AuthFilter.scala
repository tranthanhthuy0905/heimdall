
import akka.stream.Materializer
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.auth.{AuthorizationAttr, Authorizer}
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.routing.Router

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AxonAuthFilter @Inject()
  (implicit val mat: Materializer, ec: ExecutionContext, authorizer: Authorizer)
  extends Filter with LazyLogging {

  // TODO delete /media/test routes from the nonRestrictedRoutes
  final val nonRestrictedRoutes = List("/media/alive", "/media/test/create-session",  "/media/test/get-session")

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis
    if (isNonRestricted(requestHeader)) {
      executeRequest(startTime, nextFilter, requestHeader)
    } else {
      authorizer.authorize(requestHeader).flatMap {
        case Right(authData) =>
          executeRequest(startTime, nextFilter, requestHeader.addAttr(AuthorizationAttr.Key, authData))
        case Left(e) =>
          Future.successful(e)
      }
    }
  }

  private def isNonRestricted(requestHeader: RequestHeader) : Boolean = {
    nonRestrictedRoutes.contains(requestHeader.path)
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
