package services.apidae

import com.evidence.service.common.Convert

import java.util.UUID
import models.common.HeimdallRequest
import play.api.libs.json.{__, JsError, JsResult, JsSuccess, JsonValidationError, Reads, Writes}
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContent, WrappedRequest}

case class ConcatenationFile(evidenceId: UUID, fileId: UUID)

object ConcatenationFile {
  implicit val reads: Reads[ConcatenationFile] = Reads[ConcatenationFile] { json =>
    (for {
      evidenceId <- (json \ "evidence_id")
        .validate[String]
        .flatMap(id => Convert.tryToUuid(id).fold[JsResult[UUID]](JsError("error.invalid.evidence_id"))(JsSuccess(_)))
      fileId <- (json \ "file_id")
        .validate[String]
        .flatMap(id => Convert.tryToUuid(id).fold[JsResult[UUID]](JsError("error.invalid.file_id"))(JsSuccess(_)))
    } yield (evidenceId, fileId)).flatMap {
      case (evidenceId, fileId) => JsSuccess(ConcatenationFile(evidenceId, fileId))
      case _                    => JsError(JsonValidationError("error.invalid.uuid"))
    }
  }

  implicit val writes: Writes[ConcatenationFile] = (
    (__ \ "evidence_id").write[String] and
      (__ \ "file_id").write[String]
  )(f => (f.evidenceId.toString, f.fileId.toString))
}

case class ConcatenationRequest[A](
  partnerId: UUID,
  userId: UUID,
  title: String,
  files: Seq[ConcatenationFile],
  groupId: Option[UUID],
  caseIds: Option[Seq[UUID]],
  request: HeimdallRequest[A],
) extends WrappedRequest[A](request)

object ConcatenationRequest {
  implicit val writes: Writes[ConcatenationRequest[AnyContent]] = (
    (__ \ "partner_id").write[String] and
      (__ \ "user_id").write[String] and
      (__ \ "title").write[String] and
      (__ \ "files").write[Seq[ConcatenationFile]] and
      (__ \ "group_id").writeOptionWithNull[UUID] and
      (__ \ "case_ids").writeOptionWithNull[Seq[UUID]]
  )(r => (r.partnerId.toString, r.userId.toString, r.title, r.files, r.groupId, r.caseIds))
}
