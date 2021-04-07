package actions

import java.util.UUID

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import services.apidae.ApidaeRequest
import services.drd.DrdEvidenceRequest
import utils.UUIDHelper

import scala.concurrent.{ExecutionContext, Future}

case class DrdEvidenceRequestAction @Inject()()(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, DrdEvidenceRequest]
    with LazyLogging
    with UUIDHelper {

  def refine[A](request: HeimdallRequest[A]) = {

    val result = for {
      userUUID <- Convert.tryToUuid(request.subjectId).toRight(Results.BadRequest)
      partnerUUID <- Convert.tryToUuid(request.audienceId).toRight(Results.BadRequest)
      // TODO get evidenceUUID
//      evidenceUUID <- Convert.tryToUuid(request.request.getQueryString()).toRight(Results.BadRequest)
    } yield DrdEvidenceRequest(userUUID, partnerUUID, UUID.randomUUID(), request)
    Future.successful(result)
  }
}
