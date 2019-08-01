package services.rti

import java.net.URL
import java.util.UUID

import models.common.HeimdallRequest
import play.api.mvc.WrappedRequest

/**
  * RtiRequest generates request URI digestible by RTI.
  *
  * @return Generated URI as a string.
  */
case class RtiRequest[A](partnerId: UUID,
                         evidenceId: UUID,
                         fileId: UUID,
                         presignedUrl: URL,
                         watermark: String,
                         request: HeimdallRequest[A])
  extends WrappedRequest[A](request) {

}
