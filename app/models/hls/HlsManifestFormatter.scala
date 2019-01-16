package models.hls

import models.common.MediaIdent

object HlsManifestFormatter {
  def apply(manifest: String, mediaIdent: MediaIdent, pathPrefix: String, token: Option[String]): String = {
    manifest.replaceAll(
      "(?m)^/hls/", s"$pathPrefix/media/hls/"
    ).replaceAll(
      "(?m)(?<=[?&])source=[^&]*(?=($|&))", s"${mediaIdent.toQueryString}${tokenToParam(token)}"
    )
  }

  def tokenToParam(token: Option[String]): String = {
    token match {
      case Some(value) => s"&streamingSessionToken=$value"
      case _ => ""
    }
  }
}
