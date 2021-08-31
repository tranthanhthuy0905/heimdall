package services.rtm

import models.common.HeimdallRequest

/**
  * RtmRequest generates request URI digestible by RTM.
  *
  */
case class GroupRtmRequest[A](rtmRequests: Seq[RtmRequest[A]])
