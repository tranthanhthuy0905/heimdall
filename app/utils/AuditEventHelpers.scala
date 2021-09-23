package utils
import com.evidence.service.common.monad.FutureEither
import com.axon.sage.protos.query.evidence_message.EvidenceFieldSelect
import com.evidence.service.audit.Tid
import play.api.libs.ws.WSResponse
import services.audit._
import models.common.FileIdent
import services.apidae.ApidaeClient
import scala.concurrent.ExecutionContext
import play.api.mvc.BaseController
import com.evidence.service.common.logging.LazyLogging
import play.api.libs.json.JsValue
import services.sage.{EvidenceId, SageClient}
import scala.collection.mutable.{Set, ListBuffer}

trait AuditEventHelpers extends BaseController 
    with WSResponseHelpers
    with AuditConversions
    with EitherHelpers
    with LazyLogging {

    def getZipInfoAndBuildZipAccessEvent(
          sage: SageClient,
          apidae: ApidaeClient,
          auditClient: AuditClient,
          file: FileIdent,
          updatedByTid: Tid,
          remoteAddress: String,
        )(implicit ex: ExecutionContext): FutureEither[Int, Option[ZipFileAccessedEvent]] = {
          for {
            evidenceContentType <- FutureEither(sage.getEvidenceContentType(EvidenceId(file.evidenceId, file.partnerId)))
                                    .mapLeft(anyError(file))
            event <- {
              if (evidenceContentType == "application/zip") {
                FutureEither(apidae.getZipFileInfo(file.partnerId, file.evidenceId, file.fileId).map(withOKStatus))
                .map(
                  info => Some(buildZipFileAccessedEvent(info.json, file, updatedByTid, remoteAddress))
                )
              }
              else {
                FutureEither.successful(Right(None))
              }
            }
          } yield event
      }

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

    def buildZipFileAccessedEvent(json:JsValue, file: FileIdent, updatedByTid: Tid, remoteAddress: String) : ZipFileAccessedEvent = {
      val evidenceTitle = (json \ "data" \ "file_name").asOpt[String].getOrElse("")
      val filePath = (json \ "data" \ "file_path").asOpt[String].getOrElse("")
      ZipFileAccessedEvent(
        evidenceTid(file.evidenceId, file.partnerId),
        updatedByTid,
        fileTid(file.fileId, file.partnerId),
        remoteAddress,
        evidenceTitle,
        filePath
      )
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

    // this does't call to query apidae and sage
    // base on zip access, going to decide buffer event
    // file which does not have zip accessed event will have normal bufferedEvent, otherwise it will have ZipFileBufferedEvent
    def zipAccessedToBufferEvents(
      files: List[FileIdent], events : List[ZipFileAccessedEvent],
      updatedBy: Tid, 
      remoteAddress: String
    ) : List[AuditEvent] = {
      var fileHasEvent = Set[String]()
      var auditEvents = ListBuffer[AuditEvent]()
      events.foreach {
        event => {
          auditEvents += ZipFileBufferedEvent(
            event.targetTid,
            event.updatedByTid,
            event.fileTid,
            event.remoteAddress,
            event.evidenceTitle,
            event.filePath
          )
          fileHasEvent += event.fileTid.id.getOrElse("").toString
        }
      }

      files.foreach {
        file => 
        if (!fileHasEvent.contains(file.fileId.toString)) {
          auditEvents += EvidenceRecordBufferedEvent(
            evidenceTid(file.evidenceId, file.partnerId),
            updatedBy,
            fileTid(file.fileId, file.partnerId),
            remoteAddress,
          )
        }
      }
      auditEvents.toList
    }
}
