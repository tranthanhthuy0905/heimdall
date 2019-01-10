package models.common

import play.api.mvc.{Request, RequestHeader, WrappedRequest}

case class HeimdallRequest[A](rtmQuery: RtmQueryParams, request: Request[A], streamingSessionToken: Option[String])
  extends WrappedRequest[A](request)
    with RequestHeader
