package services.rtm

import akka.http.scaladsl.model.Uri
import models.common.{FileIdent, HeimdallRequest, MediaIdent}

/**
  * RtmRequest generates request URI digestible by RTM.
  *
  * @param rtmURI URI path to RTM
  * @return Generated URI as a string.
  */
class RtmRequest[A](rtmURI: Uri, updatedMedia: MediaIdent, request: HeimdallRequest[A])
    extends HeimdallRequest[A](request, request.authorizationData) {
  override def toString: String = rtmURI.toString
  override def media: MediaIdent = updatedMedia
}
