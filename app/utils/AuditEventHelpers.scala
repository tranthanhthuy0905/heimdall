package utils
import com.evidence.service.common.monad.FutureEither
import com.axon.sage.protos.query.evidence_message.EvidenceFieldSelect
import play.api.libs.ws.WSResponse
import services.audit.{AuditEvent, ZipFileAccessedEvent, ZipFileStreamedEvent}
import models.common.FileIdent
import services.apidae.ApidaeClient
import scala.concurrent.Future
import scala.concurrent.ExecutionContext 
import play.api.mvc.BaseController
import com.evidence.service.common.logging.LazyLogging
import services.sage.{SageClient, EvidenceId, QueryRequest, Evidence}

trait AuditEventHelpers extends BaseController 
    with WSResponseHelpers
    with EitherHelpers
    with LazyLogging {

    // zip/not zip review event: request apidae > decide event base on apidae's response
    def getZipInfoAndDecideReviewEvent(apidae: ApidaeClient, file: FileIdent, defaultReviewEvent: AuditEvent)(implicit ex: ExecutionContext) : FutureEither[Int, AuditEvent] = {
        FutureEither(apidae.getZipFileInfo(file.partnerId, file.evidenceId, file.fileId).map(withOKStatus)) // apidae still returns statusOk (i.e 200) when reponse.status == not_found
        .map(decideReviewEvent(defaultReviewEvent))
    }

    // decide zip/not zip review event 
    private def decideReviewEvent(baseEvidenceReviewEvent: AuditEvent)(response: WSResponse) : AuditEvent = {
        val status = (response.json \ "status").asOpt[String].getOrElse("")
        if (status == "success") {
            // if this is a zip file then use zip audit event
            val evidenceTitle = (response.json \ "data" \ "file_name").asOpt[String].getOrElse("")
            val filePath = (response.json \ "data" \ "file_path").asOpt[String].getOrElse("")
            ZipFileAccessedEvent(
                baseEvidenceReviewEvent.targetTid,
                baseEvidenceReviewEvent.updatedByTid,
                baseEvidenceReviewEvent.fileTid,
                baseEvidenceReviewEvent.remoteAddress,
                evidenceTitle,
                filePath
            )
        }
        else {
            baseEvidenceReviewEvent
        }
    }

    def getZipInfoAndDecideEvent(sage: SageClient, apidae: ApidaeClient, file: FileIdent, baseEvent: AuditEvent, zipEventBuilder: (WSResponse, AuditEvent) => AuditEvent)(implicit ex: ExecutionContext) = {
        val selection = EvidenceFieldSelect(
            partnerId = true,
            id = true,
            contentType = true
        ).namePaths().map(_.toProtoPath)

        (for {
            evidence <- FutureEither(sage.getEvidence(
                id    = EvidenceId(file.evidenceId, file.partnerId),
                query = QueryRequest(selection)
            ))
            event <- decideZipEvent(apidae, file, evidence, baseEvent, zipEventBuilder)
        } yield event).mapLeft(anyError(file))
    }

    def buildZipFileStreamedEvent(response: WSResponse, baseEvent: AuditEvent) : AuditEvent = {   
        // if this is a zip file then use zip audit event
        val evidenceTitle = (response.json \ "data" \ "file_name").asOpt[String].getOrElse("")
        val filePath = (response.json \ "data" \ "file_path").asOpt[String].getOrElse("")
        ZipFileStreamedEvent(
            baseEvent.targetTid,
            baseEvent.updatedByTid,
            baseEvent.fileTid,
            baseEvent.remoteAddress,
            evidenceTitle,
            filePath
        )
    }

    private def decideZipEvent(
        apidae: ApidaeClient, 
        file: FileIdent, 
        evidence: Evidence, 
        baseEvent: AuditEvent, 
        zipEventBuilder: (WSResponse, AuditEvent) => AuditEvent)(implicit ex: ExecutionContext) = {
        // if evidence content type is application/zip then query for file's path
        if (evidence.contentType == "application/zip") {
            FutureEither(apidae.getZipFileInfo(file.partnerId, file.evidenceId, file.fileId).map(withOKStatus)).map(response => zipEventBuilder(response, baseEvent))
        }
        else {
            FutureEither.successful(Right(baseEvent))
        }
    }

    private def anyError(file: FileIdent)(anyError: Any) = {
        // log error and forward error code to next handler
        logger.error("failedToGetZipInfoAndDecideEvent")(
            "errorCode" -> anyError,
            "file" -> file
        )
        INTERNAL_SERVER_ERROR 
    }
}
