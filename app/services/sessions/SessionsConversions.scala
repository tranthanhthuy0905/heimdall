package services.sessions

import com.evidence.service.common.finagle.FutureConverters.TwitterFutureOps
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.sessions.api.thrift.v1.{SessionsServiceErrorCode, SessionsServiceException}
import play.api.mvc.Results

import scala.concurrent.{ExecutionContext, Future}

trait SessionsConversions extends LazyLogging with Results {
  type RescueTranslator = PartialFunction[Throwable, String => SessionsServiceErrorCode]

  protected def rescue[B](description: String)
                         (future: com.twitter.util.Future[B])
                         (translator: RescueTranslator): com.twitter.util.Future[Either[SessionsServiceErrorCode, B]] = {
    future.map(u => Right(u)).handle {
      case e if translator isDefinedAt e => Left(translator(e)(description))
    }
  }

  protected def rescueSessionsCalls[T](future: com.twitter.util.Future[T], func: String)(implicit ex: ExecutionContext): Future[Either[SessionsServiceErrorCode, T]] = {
    rescue(s"$func")(future)(translator).toScalaFuture
  }

  private val translator: RescueTranslator = {
    case se: SessionsServiceException =>
      description: String =>
        logger.info("sessionsServiceException")(
          "description" -> description,
          "errorCode" -> se.errorCode,
          "message" -> se.errorMessage
        )
        se.errorCode
  }
}
