package services.rtm

import akka.http.scaladsl.model.Uri
import models.common.{HeimdallRequest, MediaIdent}

import java.net.URL

/**
  * RtmRequest generates request URI digestible by RTM.
  *
  * @param rtmURI URI path to RTM
  * @return Generated URI as a string.
  */
class RtmRequest[A](
                     rtmURI: Uri,
                     updatedMedia: MediaIdent,
                     presignedUrls: Seq[URL],
                     params: Map[String, String],
                     request: HeimdallRequest[A])
    extends HeimdallRequest[A](request, request.authorizationData) {
  override def toString: String = rtmURI.toString
  override def media: MediaIdent = updatedMedia
  def getPresignedUrls: Seq[URL] = presignedUrls
  def getParams: Map[String, String] = params
}
