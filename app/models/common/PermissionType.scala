package models.common

object PermissionType extends Enumeration {
  final val None = Value("")
  final val View = Value("view")
  final val Stream = Value("stream")
}
