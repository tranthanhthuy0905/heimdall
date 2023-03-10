package models.common

import java.util.UUID

import com.evidence.api.thrift.v1.{EntityDescriptor, TidEntities}

import scala.collection.SortedSet

/**
  * FileIdent is a primitive wrapper for an evidence identification.
  * There are cases like multicam when there can be multiple files and evidence.
  * File identifier represent a single file.
  * IMPORTANT: it is strongly recommended to use MediaIdent when possible instead of FileIdent.
  */
case class FileIdent(fileId: UUID, evidenceId: UUID, partnerId: UUID)

case class EmptyMediaIdent()
    extends MediaIdent(
      List[UUID](),
      List[UUID](),
      UUID.fromString("00000000-0000-0000-0000-000000000000")
    )

class MediaIdent(val fileIds: List[UUID], val evidenceIds: List[UUID], val partnerId: UUID) {

  def toQueryString: String = {
    if (isValid()) {
      s"file_id=${listToString(fileIds)}&evidence_id=${listToString(evidenceIds)}&partner_id=$partnerId"
    } else {
      ""
    }
  }

  def toEvidenceEntityDescriptors: List[EntityDescriptor] = {
    if (isValid()) {
      evidenceIds.map(
        evidenceId =>
          EntityDescriptor(
            TidEntities.Evidence,
            evidenceId.toString,
            Option(partnerId.toString)
        )
      )
    } else {
      List()
    }
  }

  def toFileEntityDescriptors: List[EntityDescriptor] = {
    if (isValid()) {
      fileIds.map(
        fileIds =>
          EntityDescriptor(
            TidEntities.File,
            fileIds.toString,
            Option(partnerId.toString)
        )
      )
    } else {
      List()
    }
  }

  def toList: List[FileIdent] = {
    if (isValid()) {
      (fileIds zip evidenceIds).map(x => FileIdent(x._1, x._2, partnerId))
    } else {
      List()
    }
  }

  def headOption: Option[FileIdent] = toList.headOption

  def length: Int = {
    if (isValid()) {
      fileIds.length
    } else {
      0
    }
  }

  def getSortedFileIds: SortedSet[UUID] = {
    collection.SortedSet(fileIds: _*)
  }


  private def isValid(): Boolean = {
    fileIds.nonEmpty && fileIds.length == evidenceIds.length
  }

  private def listToString(list: List[UUID]): String = {
    list.mkString(",")
  }

}
