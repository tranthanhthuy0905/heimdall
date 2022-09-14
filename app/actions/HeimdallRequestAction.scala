package actions

import javax.inject.Inject
import models.common.{AuthorizationAttr, HeimdallRequest}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class HeimdallRequestAction @Inject()(defaultParser: BodyParsers.Default)(
  implicit val ex: ExecutionContext
) extends ActionBuilder[HeimdallRequest, AnyContent]
    with ActionRefiner[Request, HeimdallRequest] {

  def refine[A](request: Request[A]): Future[Either[Result, HeimdallRequest[A]]] = {
    Future(
      request.attrs
        .get(AuthorizationAttr.Key)
        .toRight(Results.Forbidden)
        .map(HeimdallRequest(request, _)))
  }

  override def parser: BodyParsers.Default        = defaultParser
  override def executionContext: ExecutionContext = ex
}
