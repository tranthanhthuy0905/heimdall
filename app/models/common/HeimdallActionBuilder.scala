package models.common

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class HeimdallActionBuilder @Inject()(defaultParser: BodyParsers.Default)
                                     (implicit ex: ExecutionContext)
  extends ActionBuilder[HeimdallRequest, AnyContent] with LazyLogging {

  override def invokeBlock[A](request: Request[A],
                              block: HeimdallRequest[A] => Future[Result]): Future[Result] = {
    val remoteAddress = request.remoteAddress
    logger.debug("heimdallActionBuilderRemoteAddress")("remoteAddress" -> remoteAddress)
    QueryHelper(request.path, request.queryString).map( rtmQuery =>
      block(HeimdallRequest(rtmQuery, request, None))
    ).getOrElse(Future.successful(Results.BadRequest))
  }

  override def parser: BodyParsers.Default = defaultParser

  override def executionContext: ExecutionContext = ex
}
