package models.hls

import models.common.FileIdent

object HlsManifestFormatter {
  def apply(manifest: String, fileIdent: FileIdent, pathPrefix: String, token: Option[String]): String = {
    manifest.replaceAll(
      "(?m)^/hls/", s"$pathPrefix/media/hls/"
    ).replaceAll(
      "(?m)(?<=[?&])source=[^&]*(?=($|&))", s"${fileIdent.toString}${tokenToParam(token)}"
    )
  }

  def tokenToParam(token: Option[String]): String = {
    token match {
      case Some(value) => s"&streamingSessionToken=$value"
      case _ => ""
    }
  }
}
