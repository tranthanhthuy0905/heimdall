package actions

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import scala.concurrent.{ExecutionContext, Future}
import services.apidae.ApidaeRequest
import utils.UUIDHelper

case class ApidaeRequestAction @Inject()()(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, ApidaeRequest]
    with LazyLogging
    with UUIDHelper {

  def refine[A](request: HeimdallRequest[A]) = {

    val result = for {
      file     <- request.media.headOption.toRight(Results.BadRequest)
      userUUID <- Convert.tryToUuid(request.subjectId).toRight(Results.BadRequest)
    } yield ApidaeRequest(file, userUUID, request)
    Future.successful(result)
  }
}
