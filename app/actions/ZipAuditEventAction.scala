package actions

import com.evidence.service.common.monad.FutureEither
import models.common.{AuditEventType, AuthorizationAttr, FileIdent, HeimdallRequest, ZipFileMetadata}
import play.api.mvc.{ActionRefiner, Result, Results}
import services.apidae.ApidaeClient
import services.audit.{AuditConversions, AuditEvent, EvidenceLoadedForReviewEvent, EvidenceReviewEvent, ZipFileAccessedEvent}
import services.sage.{EvidenceId, SageClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ZipAuditEventActionBuilder @Inject()(sage: SageClient, apidae: ApidaeClient)(implicit val executionContext: ExecutionContext) {
  def build(auditEventType: AuditEventType.Value): ZipAuditEventAction = ZipAuditEventAction(auditEventType)(sage, apidae)(executionContext)
}


case class ZipAuditEventAction @Inject()(auditEventType: AuditEventType.Value)(sage: SageClient, apidae: ApidaeClient)(
  implicit val executionContext: ExecutionContext
) extends ActionRefiner[HeimdallRequest, HeimdallRequest]
  with AuditConversions {

  def refine[A](input: HeimdallRequest[A]): Future[Either[Result, HeimdallRequest[A]]] = {
    (for {
      mediaFile <- FutureEither.successful(input.media.headOption.toRight(Results.BadRequest))
      isZipFile <- isZipEvidence(mediaFile)
    } yield isZipFile
    )
    .flatMap {
      case true =>
        refinedWithZipAudit(auditEventType)(input)
      case false => FutureEither.successful(Right(input))
    }.future
  }

  private def isZipEvidence[A](mediaFile: FileIdent) = {
    for {
      evidenceContentType <- FutureEither(sage.getEvidenceContentType(EvidenceId(mediaFile.evidenceId, mediaFile.partnerId))).mapLeft(_ => Results.InternalServerError)
    } yield evidenceContentType equals "application/zip"
  }

  private def refinedWithZipAudit[A](auditEventType: AuditEventType.Value)(input: HeimdallRequest[A]) = {
    for {
      mediaFile <- FutureEither.successful(input.media.headOption.toRight(Results.BadRequest))
      zipFileMetadata <- FutureEither.fromFuture(
        apidae.getZipFileInfo(mediaFile.partnerId, mediaFile.evidenceId, mediaFile.fileId))(_ => Results.InternalServerError)
    } yield input.copy(auditEvent = Some(toAuditEvent(auditEventType, input, zipFileMetadata)))

  }

  private def toAuditEvent[A](eventType: AuditEventType.Value, input: HeimdallRequest[A], zipFileMetadata: ZipFileMetadata): AuditEvent = {
    val authHandler = input.attrs(AuthorizationAttr.Key)

    eventType match {
        case AuditEventType.ZipFileReviewed =>
          ZipFileAccessedEvent(
            evidenceTid(zipFileMetadata.evidenceId, zipFileMetadata.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(zipFileMetadata.fileId, zipFileMetadata.partnerId),
            input.clientIpAddress,
            zipFileMetadata.fileName,
            zipFileMetadata.filePath
          )
        case AuditEventType.ZipEvidenceLoaded =>
          EvidenceLoadedForReviewEvent(
            evidenceTid(zipFileMetadata.evidenceId, zipFileMetadata.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(zipFileMetadata.fileId, zipFileMetadata.partnerId),
            input.clientIpAddress
          )
    }
  }
}
