package actions

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.auth.StreamingSessionData
import models.common.HeimdallRequest
import play.api.mvc.{ActionFilter, Results}

import scala.concurrent.{ExecutionContext, Future}

case class TokenValidationAction @Inject()(sessionData: StreamingSessionData)(
  implicit val executionContext: ExecutionContext
) extends ActionFilter[HeimdallRequest]
    with LazyLogging {

  def filter[A](request: HeimdallRequest[A]) = Future.successful {
    val result =
      sessionData.validateToken(
        request.streamingSessionToken,
        request.cookie,
        request.media.getSortedFileIds
      )

    result match {
      case true =>
        None
      case _ => {
        logger.error("invalidOrMissingStreamingSessionToken")(
          "streamingSessionToken" -> request.streamingSessionToken,
          "path"                  -> request.path,
          "query"                 -> request.queryString,
          "cookie"                -> request.cookie
        )
        Some(Results.Forbidden)
      }
    }
  }
}
