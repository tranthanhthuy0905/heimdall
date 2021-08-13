package utils
import com.evidence.service.common.monad.FutureEither
import play.api.libs.ws.WSResponse
import services.audit.{AuditEvent, ZipFileAccessedEvent}
import models.common.FileIdent
import services.apidae.ApidaeClient
import scala.concurrent.Future
import scala.concurrent.ExecutionContext 
import play.api.mvc.BaseController

trait AuditEventHelpers extends BaseController with WSResponseHelpers {

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
}
