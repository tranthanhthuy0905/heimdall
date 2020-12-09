package actions

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import play.api.mvc.{ActionFilter, Result, Results}
import services.apidae.ApidaeRequest
import utils.UUIDHelper

import scala.concurrent.{ExecutionContext, Future}

case class MediaConvertValidation @Inject()()(implicit val executionContext: ExecutionContext)
    extends ActionFilter[ApidaeRequest]
    with LazyLogging
    with UUIDHelper {

  def filter[A](req: ApidaeRequest[A]): Future[Option[Result]] = {
    // EVP-1798 not allow transcode request from different agency
    Future {
      // req.request.audienceId = jwt audienceId ~ partner_id of current user
      if (equalsUUIDString(req.file.partnerId.toString, req.request.audienceId)) {
        None
      } else {
        Some(Results.Forbidden)
      }
    }
  }

  def equalsUUIDString(a: String, b: String): Boolean = {
    a.replaceAll("-", "").equalsIgnoreCase(b.replaceAll("-", ""))
  }
}
