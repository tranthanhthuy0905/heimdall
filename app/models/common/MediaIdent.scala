package models.common

import java.util.UUID

import scala.collection.SortedSet

case class FileIdent(fileId: UUID, evidenceId: UUID, partnerId: UUID)

case class MediaIdent(fileIds: List[UUID], evidenceIds: List[UUID], partnerId: UUID) {
  def toQueryString: String = {
    if (isValid(fileIds, evidenceIds)) {
      s"file_id=${listToString(fileIds)}&evidence_id=${listToString(evidenceIds)}&partner_id=$partnerId"
    } else {
      ""
    }
  }

  def toList: List[FileIdent] = {
    if (isValid(fileIds, evidenceIds)) {
      (fileIds zip evidenceIds).map(x => FileIdent(x._1, x._2, partnerId))
    } else {
      List()
    }
  }

  def length: Int = {
    if (isValid(fileIds, evidenceIds)) {
      fileIds.length
    } else {
      0
    }
  }

  def getSortedFileIds: SortedSet[UUID] = {
    collection.SortedSet(fileIds: _*)
  }

  private def isValid(fileIds: List[UUID], evidenceIds: List[UUID]): Boolean = {
    fileIds.length == evidenceIds.length && fileIds.length > 0
  }

  private def listToString(list: List[UUID]): String = {
    list.mkString(",")
  }

}
