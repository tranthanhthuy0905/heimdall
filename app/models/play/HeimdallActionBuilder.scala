package models.play

import javax.inject.Inject
import models.common.{QueryHelper, RtmQueryParams}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class HeimdallRequest[A](rtmQuery: RtmQueryParams, request: Request[A]) extends WrappedRequest[A](request) with RequestHeader

class HeimdallActionBuilder @Inject()(defaultParser: BodyParsers.Default)
                                     (implicit ex: ExecutionContext)
  extends ActionBuilder[HeimdallRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A],
                              block: HeimdallRequest[A] => Future[Result]): Future[Result] = {
    QueryHelper(request.path, request.queryString).map(
      extendedQuery => block(HeimdallRequest(extendedQuery, request))
    ).getOrElse(Future.successful(Results.BadRequest))
  }

  override def parser: BodyParsers.Default = defaultParser

  override def executionContext: ExecutionContext = ex
}
