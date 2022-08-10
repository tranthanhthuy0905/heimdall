package services.sage

import models.common.{FileIdent, HeimdallRequest}
import com.axon.sage.protos.v1.evidence_video_service.{ExtractionStatus, ConvertedFile => SageConvertedFileProto}
import com.axon.sage.protos.common.common.Tid
import com.axon.sage.protos.query.file_message.File
import com.axon.sage.protos.query.file_message.File.Status
import com.google.protobuf.duration.Duration
import com.google.protobuf.timestamp.Timestamp
import play.api.libs.json._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{JsString, JsValue, Writes}

import java.time.Instant

import java.util.UUID
import play.api.mvc.WrappedRequest

case class ConvertedFilesRequest[A](file: FileIdent, request: HeimdallRequest[A])
  extends WrappedRequest[A](request)

case class ConvertedFile(evidenceId: UUID,
                         partnerId: UUID,
                         displayName: String,
                         contentType: String,
                         duration: Duration,
                         fileName: String,
                         owner: Tid,
                         status: Status,
                         extractions: Seq[ExtractionStatus],
                         index: Int
                        )

object ConvertedFile {
  def fromSageProto(convertedFile: SageConvertedFileProto) : ConvertedFile = {
    new ConvertedFile(
      UUID.fromString(convertedFile.id),
      UUID.fromString(convertedFile.partnerId),
      convertedFile.displayName.getOrElse(""),
      convertedFile.contentType,
      convertedFile.duration.getOrElse(Duration.of(0, 0)),
      convertedFile.fileName.getOrElse(""),
      convertedFile.owner.getOrElse(Tid.defaultInstance),
      convertedFile.status,
      convertedFile.extractions,
      convertedFile.index
    )
  }

  def convertedFilesToJsonValue(files: Seq[ConvertedFile]): JsValue = Json.toJson(files map toJsonObj)

  def toJsonObj(file: ConvertedFile): JsObject = {
    Json.obj(
      "id" -> file.evidenceId,
      "displayName" -> file.displayName,
      "contentType" -> file.contentType,
      "duration" -> toJson(file.duration),
      "fileName" -> file.fileName,
      "owner" -> toJson(file.owner),
      "status" -> toString(file.status),
      "extractions" -> extractionStatusesToJsValue(file.extractions),
      "index" -> file.index
    )
  }

  def extractionStatusesToJsValue(extractionStatuses: Seq[ExtractionStatus]): JsValue =
    Json.toJson(extractionStatuses map toJsonObj)

  def toJsonObj(extractionStatus: ExtractionStatus): JsObject = {
    Json.obj(
      "id" -> extractionStatus.id,
      "status" -> extractionStatus.status,
      "displayName" -> extractionStatus.displayName,
      "dateCreated" -> timestampToJson(extractionStatus.dateCreated),
      "dateUpdated" -> timestampToJson(extractionStatus.dateUpdated),
      "destinationEvidenceId" -> extractionStatus.destinationEvidenceId,
      "destinationFileId" -> extractionStatus.destinationFileId,
      "subscriberId" -> extractionStatus.subscriberId,
      "index" -> extractionStatus.index
    )
  }

  def toJson(duration: Duration): JsValue = {
    Json.toJson(java.time.Duration.
      ofSeconds(duration.seconds).
      withNanos(duration.nanos).toNanos)
  }

  def toJson(tid: Tid): JsValue = {
    Json.toJson(s"${toString(tid.entityType)}:${tid.entityId}@${tid.entityDomain}")
  }

  private def toString(entityType: Tid.EntityType): String = {
    entityType match {
      case Tid.EntityType.SUBSCRIBER => "subscriber"
      case Tid.EntityType.PARTNER => "partner"
      case Tid.EntityType.CASE => "case"
      case Tid.EntityType.EVIDENCE => "evidence"
      case Tid.EntityType.FILE => "file"
      case Tid.EntityType.TEAM => "team"
      case Tid.EntityType.DEVICE => "device"
      case Tid.EntityType.DSN => "dsn"
      case Tid.EntityType.WORKER => "worker"
      case Tid.EntityType.AUTH_CLIENT => "auth_client"
      case Tid.EntityType.UNKNOWN => "unknown"
      case _ => "unknown"
    }
  }

  private def toString(fileStatus: File.Status): String = {
    fileStatus match {
      case File.Status.START => "start"
      case File.Status.AVAILABLE => "available"
      case File.Status.PENDING_DELETION => "pending_deletion"
      case File.Status.DELETED => "deleted"
      case File.Status.ERRORED => "errored"
      case File.Status.PURGED => "purged"
      case _ => "unknown"
    }
  }

  implicit val timestampWrites: Writes[Timestamp] = (ts: Timestamp) => {
    val dateTime = new DateTime(Instant.ofEpochSecond(ts.seconds, ts.nanos).toEpochMilli, DateTimeZone.UTC)
    JsString(dateTime.toString(ISODateTimeFormat.dateHourMinuteSecondMillis()) + "Z")
  }

  private def timestampToJson(time: Option[Timestamp]): JsValue = {
    time match {
      case Some(value) => Json.toJson(value)
      case _ => Json.toJson("")
    }
  }
}