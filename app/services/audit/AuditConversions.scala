package services.audit

import java.util.UUID

import com.evidence.api.thrift.v1.TidEntities
import com.evidence.service.audit.Tid
import models.auth.JWTWrapper

trait AuditConversions {
  def fileTid(fileId: UUID,  partnerId: UUID): Tid = {
    Tid(
      TidEntities.File,
      Some(fileId.toString),
      Some(partnerId.toString)
    )
  }

  def evidenceTid(evidenceId: UUID, partnerId: UUID): Tid = {
    Tid(
      TidEntities.Evidence,
      Some(evidenceId.toString),
      Some(partnerId.toString)
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
