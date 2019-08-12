package filters

import akka.stream.Materializer
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import javax.inject.Inject
import models.auth.Authorizer
import models.common.{AuthorizationAttr, MediaIdent, MediaIdentAttr}
import play.api.mvc.{Filter, RequestHeader, Result, Results}
import play.api.routing.Router
import utils.UUIDHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/** HeimdallRequestFilter performs the following actions:
  * - Request authorization.
  *   - Get AXONSESSION cookie.
  *   - Send AXONSESSION cookie to sessions-service to obtain JWT.
  *   - Parse JWT and wrap into AuthorizationData, which will be stored as request attribute.
  * - Extraction of required query parameters.
  *   - Extract required query parameters from the queryString as UUIDs (media identifiers - MediaIdent).
  *   - Store MediaIdent as request attribute.
  */
class HeimdallRequestFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext, authorizer: Authorizer)
    extends Filter
    with LazyLogging
    with StrictStatsD
    with UUIDHelper {

  /** List of routes for internal usage only, such as heartbeat.
    *
    * nginx must deny access to Heimdall's endpoints listed as Internals.
    * See the `deny` access directive of nginx.
    */
  final val InternalRoutes = List("/media/alive")

  def apply(
    nextFilter: RequestHeader => Future[Result]
  )(requestHeader: RequestHeader): Future[Result] = {
    val startTime  = System.currentTimeMillis
    val actionName = this.getActionName(requestHeader)

    executionTime(s"$actionName.time") {
      logger.info("requestBegin")(
        "action" -> actionName,
        "path"   -> requestHeader.path,
        "query"  -> requestHeader.queryString
      )
      if (InternalRoutes.contains(requestHeader.path)) {
        executeRequest(startTime, nextFilter, requestHeader, actionName)
      } else {
        authorizer.authorize(requestHeader).flatMap {
          case Right(authData) =>
            val maybeMediaIdent = this.getMediaIdent(requestHeader)
            maybeMediaIdent match {
              case Some(mediaIdent) =>
                executeRequest(
                  startTime,
                  nextFilter,
                  requestHeader
                    .addAttr(AuthorizationAttr.Key, authData)
                    .addAttr(MediaIdentAttr.Key, mediaIdent),
                  actionName
                )
              case None =>
                val details =
                  "one or more parameters were not provided in incoming request"
                logger.info("badRequest")(
                  "action"      -> actionName,
                  "path"        -> requestHeader.path,
                  "queryString" -> requestHeader.queryString,
                  "details"     -> details
                )
                Future.successful(Results.BadRequest)
            }
          case Left(e) =>
            Future.successful(e)
        }
      }
    }
  }

  private def executeRequest(
    startTime: Long,
    nextFilter: RequestHeader => Future[Result],
    requestHeader: RequestHeader,
    actionName: String): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      val requestTime = System.currentTimeMillis - startTime
      logger.info("requestComplete")(
        "action"   -> actionName,
        "path"     -> requestHeader.path,
        "query"    -> requestHeader.queryString,
        "duration" -> s"${requestTime}ms",
        "status"   -> result.header.status,
      )
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

  private def getMediaIdent(
    requestHeader: RequestHeader
  ): Option[MediaIdent] = {
    for {
      fileIds     <- getUuidListByKey("file_id", requestHeader.queryString)
      evidenceIds <- getUuidListByKey("evidence_id", requestHeader.queryString)
      partnerId   <- getUuidValueByKey("partner_id", requestHeader.queryString)
    } yield new MediaIdent(fileIds, evidenceIds, partnerId)
  }

}
