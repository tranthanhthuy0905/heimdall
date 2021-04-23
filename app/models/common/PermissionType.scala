package models.common

object PermissionType extends Enumeration {
  final val None              = Value("")
  final val View              = Value("view")
  final val Stream            = Value("stream")
  final val FileStream        = Value("file.stream")
  final val EvidenceView      = Value("evidence.view")
  final val EvidenceViewBasic = Value("evidence.viewBasic")
  final val PartnerEdit = Value("partner.edit")
  final val PartnerAnyRead = Value("partner.any.read")
}
