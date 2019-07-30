package actions

import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class HeimdallRequestAction @Inject()(defaultParser: BodyParsers.Default)(
  implicit val ex: ExecutionContext
) extends ActionBuilder[HeimdallRequest, AnyContent]
    with ActionTransformer[Request, HeimdallRequest] {
  def transform[A](input: Request[A]): Future[HeimdallRequest[A]] = {
    Future.successful(HeimdallRequest(input))
  }

  override def parser: BodyParsers.Default = defaultParser
  override def executionContext: ExecutionContext = ex
}
