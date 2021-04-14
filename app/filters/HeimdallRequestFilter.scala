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
import java.time.Duration

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
  final val internalRoutes       = List("/media/alive")
  final val redactionRoutePrefix = "/api/v1/redaction/"

  def apply(
    nextFilter: RequestHeader => Future[Result]
  )(requestHeader: RequestHeader): Future[Result] = {
    val startTime          = System.currentTimeMillis
    val actionName: String = this.getActionName(requestHeader)
    executionTime(
      s"$actionName.time",
      authAndExecuteRequest(startTime, nextFilter, requestHeader, actionName),
      includeServiceLevelLatencyStat = true)
  }

  private def authAndExecuteRequest(
    startTime: Long,
    nextFilter: RequestHeader => Future[Result],
    requestHeader: RequestHeader,
    actionName: String): Future[Result] = {
    if (internalRoutes.contains(requestHeader.path)) {
      executeRequest(startTime, System.currentTimeMillis, nextFilter, requestHeader, actionName)
    } else if (requestHeader.path.startsWith(redactionRoutePrefix)) {
      // if redaction APIs
      authorizer.authorize(requestHeader).flatMap {
        case Right(authData) =>
          executeRequest(
            startTime,
            System.currentTimeMillis,
            nextFilter,
            requestHeader
              .addAttr(AuthorizationAttr.Key, authData),
            actionName
          )
        case Left(e) =>
          Future.successful(e)
      }
    } else {
      authorizer.authorize(requestHeader).flatMap {
        case Right(authData) =>
          val maybeMediaIdent = this.getMediaIdent(requestHeader)
          maybeMediaIdent match {
            case Some(mediaIdent) =>
              executeRequest(
                startTime,
                System.currentTimeMillis,
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

  private val logInterval = Duration.ofSeconds(6)
  private val metricInterval = Duration.ofSeconds(31)
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
      // TODO temporary metric to investigate VIC-156/VIC-94, it is safe to remove after the ticket is closed
      statsd.recordExecutionTime(s"$actionName.temp.execution_time",
       requestTime,
       s"status:${result.header.status.toString.toLowerCase}")
      // TODO temporary metric to record all request that have execution time <31 seconds
      if (requestTime <= metricInterval.toMillis) {
        statsd.recordExecutionTime(s"$actionName.temp.execution_time.31sec",
        requestTime,
        s"status:${result.header.status.toString.toLowerCase}")
      }

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
