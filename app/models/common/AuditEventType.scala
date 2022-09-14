package models.common

import services.audit.VideoConcatenationRequestedEvent

object AuditEventType extends Enumeration {
  final val EvidenceReviewed = Value("evidence_reviewed")

  final val EvidenceConversionRequested = Value("evidence_conversion_requested")
  final val VideoConcatenationRequested = Value("video_concatenation_requested")


  final val ZipFileReviewed = Value("zip_file_reviewed")
  final val ZipEvidenceLoaded = Value("zip_evidence_loaded")
}
