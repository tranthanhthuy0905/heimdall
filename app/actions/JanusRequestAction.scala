package actions

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import scala.concurrent.{ExecutionContext, Future}
import services.janus.JanusRequest
import utils.UUIDHelper

case class JanusRequestAction @Inject()(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, JanusRequest]
    with LazyLogging
    with UUIDHelper {

  def refine[A](request: HeimdallRequest[A]) = {

    val result = for {
      file     <- request.media.headOption.toRight(Results.BadRequest)
      userId   <- request.parsedJwt.map(_.subjectId).toRight(Results.BadRequest)
      userUUID <- Convert.tryToUuid(userId).toRight(Results.BadRequest)
    } yield JanusRequest(file, userUUID, request)
    Future.successful(result)
  }
}
