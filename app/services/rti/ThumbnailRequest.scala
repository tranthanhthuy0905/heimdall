package services.rti

import java.net.URL

import models.common.{FileIdent, HeimdallRequest}
import play.api.mvc.WrappedRequest

/**
  * ThumbnailRequest generates request URI digestible by RTI to extract image thumbnail.
  *
  * @return Generated URI as a string.
  */
case class ThumbnailRequest[A](file: FileIdent, presignedUrl: URL, width: Int, height: Int, request: HeimdallRequest[A])
  extends WrappedRequest[A](request)
