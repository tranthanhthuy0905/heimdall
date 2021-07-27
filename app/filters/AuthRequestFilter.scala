package filters

import akka.stream.Materializer
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.auth.Authorizer
import models.common.AuthorizationAttr
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

/** AuthRequestFilter performs the following actions:
  * - Request authorization.
  *   - Get AXONSESSION cookie.
  *   - Send AXONSESSION cookie to sessions-service to obtain JWT.
  *   - Parse JWT and wrap into AuthorizationData, which will be stored as request attribute.
  */
class AuthRequestFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext, authorizer: Authorizer)
  extends Filter {

  /** List of routes for internal usage only, such as heartbeat.
    *
    * nginx must deny access to Heimdall's endpoints listed as Internals.
    * See the `deny` access directive of nginx.
    */
  final val internalRoutes       = List("/media/alive")
  final val agenciesSettingsPrefix = "/media/settings/agencies"

  def apply(
             nextFilter: RequestHeader => Future[Result]
           )(requestHeader: RequestHeader): Future[Result] = {
    Option(requestHeader).filter(_ => internalRoutes.contains(requestHeader.path)).map(nextFilter)
      .getOrElse(authRequest(requestHeader, nextFilter))
  }

  private def authRequest(requestHeader: RequestHeader, nextFilter: RequestHeader => Future[Result]):Future[Result] = {
      FutureEither(authorizer.authorize(requestHeader)).map(requestHeader.addAttr(AuthorizationAttr.Key, _))
        .fold(l => Future.successful(l), r => nextFilter(r))
        .flatten
  }
}
