package utils
import play.api.libs.ws.WSResponse
import services.audit.{AuditEvent, ZipFileAccessedEvent}

trait AuditEventHelpers {
    // decide zip/not zip review event to be audited based on reponse from apidae
    def decideReviewEvent(response: WSResponse, baseEvidenceReviewEvent: AuditEvent) : AuditEvent = {
        val status = (response.json \ "status").asOpt[String].getOrElse("")
        // if this is a zip file then use zip audit event
        if (status == "success") {
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