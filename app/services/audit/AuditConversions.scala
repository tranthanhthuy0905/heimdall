package services.audit

import com.evidence.api.thrift.v1.TidEntities
import com.evidence.service.audit.Tid
import models.auth.JWTWrapper
import models.common.FileIdent

trait AuditConversions {
  def fileTid(file: FileIdent): Tid = {
    Tid(
      TidEntities.File,
      Some(file.fileId.toString),
      Some(file.partnerId.toString)
    )
  }

  def evidenceTid(file: FileIdent): Tid = {
    Tid(
      TidEntities.Evidence,
      Some(file.evidenceId.toString),
      Some(file.partnerId.toString)
    )
  }

  def updatedByTid(jwtWrapper: JWTWrapper): Tid = {
    val maybeEntity = TidEntities.valueOf(jwtWrapper.subjectType)
    Tid(
      maybeEntity.getOrElse(TidEntities.Unknown),
      Some(jwtWrapper.subjectId),
      Some(jwtWrapper.audienceId)
    )
  }

}
