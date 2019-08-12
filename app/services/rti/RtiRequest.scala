package services.rti

import java.net.URL

import models.common.{FileIdent, HeimdallRequest}
import play.api.mvc.WrappedRequest

/**
  * RtiRequest generates request URI digestible by RTI.
  *
  * @return Generated URI as a string.
  */
case class RtiRequest[A](file: FileIdent, presignedUrl: URL, watermark: String, request: HeimdallRequest[A])
    extends WrappedRequest[A](request)
