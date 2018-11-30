package models.common

import java.util.UUID

case class FileIdent (fileId: UUID, evidenceId: UUID, partnerId: UUID) {
  override def toString: String = {
    s"file_id=$fileId&evidence_id=$evidenceId&partner_id=$partnerId"
  }
}
