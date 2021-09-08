package services.metadata

import models.common.{FileIdent, HeimdallRequest}
import play.api.mvc.WrappedRequest

case class MetadataRequest[A](file: FileIdent, snapshotVersion: String, request: HeimdallRequest[A])
    extends WrappedRequest[A](request)
