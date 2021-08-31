package services.rtm

import models.common.HeimdallRequest

class GroupRtmRequest[A](rtmRequests: Seq[RtmRequest[A]], request: HeimdallRequest[A])
  extends HeimdallRequest[A](request, request.authorizationData) {
  def toList = rtmRequests
}