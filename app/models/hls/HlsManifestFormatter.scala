package models.hls

import models.common.FileIdent

object HlsManifestFormatter {
  def apply(manifest: String, fileIdent: FileIdent, pathPrefix: String): String = {
    manifest.replaceAll("(?m)^/hls/", s"$pathPrefix/media/hls/").replaceAll(
      "(?m)(?<=[?&])source=[^&]*(?=($|&))", fileIdent.toString
    )
  }
}
