package models.common

import play.api.mvc.{Request, WrappedRequest}

case class HeimdallRequest[A](rtmQuery: RtmQueryParams, request: Request[A], streamingSessionToken: Option[String])
  extends WrappedRequest[A](request)

case class HeimdallRtiRequest[A](rtiQuery: RtiQueryHelper, request: Request[A])
  extends WrappedRequest[A](request)
