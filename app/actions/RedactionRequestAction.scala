package actions

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import services.drd.DrdEvidenceRequest
import services.pdp.PdpClient
import utils.UUIDHelper

import scala.concurrent.{ExecutionContext, Future}

case class RedactionRequestActionBuilder @Inject()(pdp: PdpClient)(implicit val executionContext: ExecutionContext) {

  def build(evidenceId: String) = {
    RedactionRequestAction(evidenceId)(executionContext)
  }
}

case class RedactionRequestAction @Inject()(evidenceId: String)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, DrdEvidenceRequest]
    with LazyLogging
    with UUIDHelper {

  def refine[A](request: HeimdallRequest[A]) = {

    val result = for {
      userUUID     <- Convert.tryToUuid(request.subjectId).toRight(Results.BadRequest)
      partnerUUID  <- Convert.tryToUuid(request.audienceId).toRight(Results.BadRequest)
      evidenceUUID <- Convert.tryToUuid(evidenceId).toRight(Results.BadRequest)
    } yield DrdEvidenceRequest(userUUID, partnerUUID, evidenceUUID, request)
    Future.successful(result)
  }
}
