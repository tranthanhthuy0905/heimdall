package actions

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging
import play.api.mvc.{ActionFilter, Result, Results}
import services.apidae.{ApidaeRequest, ConcatenationRequest}
import utils.UUIDHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ConcatenationValidation @Inject()()(implicit val executionContext: ExecutionContext)
    extends ActionFilter[ConcatenationRequest]
    with LazyLogging
    with UUIDHelper {

  def filter[A](req: ConcatenationRequest[A]): Future[Option[Result]] = {
    Future.successful(if (equalsUUIDString(req.partnerId.toString, req.request.audienceId)) None else Some(Results.Forbidden))
  }

  def equalsUUIDString(a: String, b: String): Boolean = Convert.tryToUuid(a).equals(Convert.tryToUuid(b))
}
