package models.common

object AuditEventType extends Enumeration {
  final val EvidenceReviewed = Value("evidence_reviewed")
  final val ZipFileReviewed = Value("zip_file_reviewed")
}
