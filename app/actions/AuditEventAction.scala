package actions

import com.evidence.service.common.monad.FutureEither
import models.common.{AuditEventType, AuthorizationAttr, HeimdallRequest}
import play.api.mvc.{ActionRefiner, Result, Results}
import services.audit.{AuditConversions, AuditEvent, EvidencePlaybackRequested, EvidenceReviewEvent, VideoConcatenationRequestedEvent}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class AuditEventActionBuilder @Inject()()(implicit val executionContext: ExecutionContext) {
  def build(auditEventType: AuditEventType.Value): AuditEventAction = AuditEventAction(auditEventType)()(executionContext)
}

case class AuditEventAction @Inject()(auditEventType: AuditEventType.Value)()(
  implicit val executionContext: ExecutionContext
) extends ActionRefiner[HeimdallRequest, HeimdallRequest]
  with AuditConversions {

  def refine[A](input: HeimdallRequest[A]): Future[Either[Result, HeimdallRequest[A]]] = {
    FutureEither(
      toAuditEvent(auditEventType)(input))
        .map(auditEvent => input.copy(auditEvent = Some(auditEvent))
    ).future
  }


  private def toAuditEvent[A](eventType: AuditEventType.Value)(input: HeimdallRequest[A]): Future[Either[Result, AuditEvent]] = {
    val authHandler = input.attrs(AuthorizationAttr.Key)
    Future {
      input.media.headOption.map(file => eventType match {
        case AuditEventType.EvidenceReviewed =>
          EvidenceReviewEvent(
            evidenceTid(file.evidenceId, file.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(file.fileId, file.partnerId),
            input.clientIpAddress
          )
        case AuditEventType.EvidenceConversionRequested =>
          EvidencePlaybackRequested(
            evidenceTid(file.evidenceId, file.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(file.fileId, file.partnerId),
            input.clientIpAddress
          )


      }).map(Right(_)).getOrElse(Left(Results.BadRequest))
    }

  }



}
