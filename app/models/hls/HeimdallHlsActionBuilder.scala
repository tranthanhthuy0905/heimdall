package models.hls

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.auth.{AuthorizationAttr, StreamingSessionData}
import models.common.{HeimdallRequest, QueryHelper}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class HeimdallHlsActionBuilder @Inject()(defaultParser: BodyParsers.Default)
                                              (implicit ex: ExecutionContext, sessionData: StreamingSessionData)
  extends ActionBuilder[HeimdallRequest, AnyContent] with LazyLogging {

  override def invokeBlock[A](request: Request[A],
                              block: HeimdallRequest[A] => Future[Result]): Future[Result] = {
    QueryHelper(request.path, request.queryString) match {
      case Some(rtmQuery) =>
        getStreamingSessionToken(request) match {
          case Some(streamingSessionToken) =>
            val authHandler = request.attrs(AuthorizationAttr.Key)
            if (sessionData.validateToken(streamingSessionToken, authHandler.token, rtmQuery.media.getSortedFileIds)) {
              block(HeimdallRequest(rtmQuery, request, Some(streamingSessionToken)))
            } else {
              logger.error("invalidStreamingSessionToken")("streamingSessionToken" -> streamingSessionToken, "query" -> request.queryString)
              Future.successful(Results.Forbidden)
            }
          case None =>
            logger.error("missingStreamingSessionToken")(
              "path"-> request.path,
              "query" -> request.queryString
            )
            Future.successful(Results.Forbidden)
        }
      case None =>
        logger.error("malformedRequestQuery")("query" -> request.queryString)
        Future.successful(Results.BadRequest)
    }
  }

  private def getStreamingSessionToken[A](request: Request[A]) : Option[String] = {
    for {
      tokenSeq <- request.queryString.get("streamingSessionToken")
      token <- tokenSeq.headOption
    } yield token
  }

  override def parser: BodyParsers.Default = defaultParser

  override def executionContext: ExecutionContext = ex
}
