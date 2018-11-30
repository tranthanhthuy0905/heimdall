package models.play

import javax.inject.Inject
import models.common.{QueryValidator, ValidatedQuery}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class HeimdallRequest[A](val validatedQuery: ValidatedQuery, request: Request[A]) extends WrappedRequest[A](request)

class  HeimdallActionBuilder @Inject()(defaultParser: BodyParsers.Default)
                                (implicit ex: ExecutionContext)
  extends ActionBuilder[HeimdallRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A],
                              block: HeimdallRequest[A] => Future[Result]): Future[Result] = {
    QueryValidator(request.path, request.queryString).map(
      validatedQuery => block(HeimdallRequest(validatedQuery, request))
    ).getOrElse(Future.successful(Results.BadRequest))
  }

  override def parser = defaultParser

  override def executionContext = ex
}