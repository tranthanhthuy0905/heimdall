package models.common

import javax.inject.Inject
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class HeimdallActionBuilder @Inject()(defaultParser: BodyParsers.Default)
                                     (implicit ex: ExecutionContext)
  extends ActionBuilder[HeimdallRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A],
                              block: HeimdallRequest[A] => Future[Result]): Future[Result] = {
    QueryHelper(request.path, request.queryString).map(rtmQuery =>
      block(HeimdallRequest(rtmQuery, request, None))
    ).getOrElse(Future.successful(Results.BadRequest))
  }

  override def parser: BodyParsers.Default = defaultParser

  override def executionContext: ExecutionContext = ex
}

class HeimdallRtiActionBuilder @Inject()(defaultParser: BodyParsers.Default)
                                        (implicit ex: ExecutionContext)
  extends ActionBuilder[HeimdallRtiRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A],
                              block: HeimdallRtiRequest[A] => Future[Result]): Future[Result] = {
    RtiQueryHelper(request.path, request.queryString).map(rtiQuery =>
      block(HeimdallRtiRequest(rtiQuery, request))
    ).getOrElse(Future.successful(Results.BadRequest))
  }

  override def parser: BodyParsers.Default = defaultParser

  override def executionContext: ExecutionContext = ex
}