package utils
import com.evidence.service.common.monad.FutureEither
import com.axon.sage.protos.query.evidence_message.EvidenceFieldSelect
import play.api.libs.ws.WSResponse
import services.audit.{AuditEvent, ZipFileAccessedEvent, ZipFileStreamedEvent, ZipFileBufferedEvent}
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

    def getZipInfoAndDecideEvent(sage: SageClient, apidae: ApidaeClient, file: FileIdent, baseEvent: AuditEvent, zipEventBuilder: (WSResponse, AuditEvent) => AuditEvent)(implicit ex: ExecutionContext) = {
        val selection = EvidenceFieldSelect(
            partnerId = true,
            id = true,
            contentType = true
        ).namePaths().map(_.toProtoPath)

        (for {
            evidenceContentType <- FutureEither(sage.getEvidenceContentType(EvidenceId(file.evidenceId, file.partnerId)))
            event <- decideZipEvent(apidae, file, evidenceContentType, baseEvent, zipEventBuilder)
        } yield event).mapLeft(anyError(file))
    }

    def buildZipFileAccessedEvent(response: WSResponse, baseEvent: AuditEvent) : AuditEvent = {   
        // if this is a zip file then use zip audit event
        val evidenceTitle = (response.json \ "data" \ "file_name").asOpt[String].getOrElse("")
        val filePath = (response.json \ "data" \ "file_path").asOpt[String].getOrElse("")
        ZipFileAccessedEvent(
            baseEvent.targetTid,
            baseEvent.updatedByTid,
            baseEvent.fileTid,
            baseEvent.remoteAddress,
            evidenceTitle,
            filePath
        )
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

    def buildZipFileBufferedEvent(response: WSResponse, baseEvent: AuditEvent) : AuditEvent = {   
        // if this is a zip file then use zip audit event
        val evidenceTitle = (response.json \ "data" \ "file_name").asOpt[String].getOrElse("")
        val filePath = (response.json \ "data" \ "file_path").asOpt[String].getOrElse("")
        ZipFileBufferedEvent(
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
        evidenceContentType: String, 
        baseEvent: AuditEvent, 
        zipEventBuilder: (WSResponse, AuditEvent) => AuditEvent)(implicit ex: ExecutionContext) = {
        // if evidence content type is application/zip then query for file's path
        if (evidenceContentType == "application/zip") {
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
