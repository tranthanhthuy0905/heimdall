package models.common

import java.util.UUID

case class ZipFileMetadata(
                            partnerId: UUID,
                            evidenceId: UUID,
                            fileId: UUID,
                            fileName: String,
                            filePath: String
                          )
