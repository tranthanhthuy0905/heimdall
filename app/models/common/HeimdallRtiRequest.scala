package models.common

import play.api.mvc.{Request, WrappedRequest}

case class HeimdallRtiRequest[A](rtiQuery: RtiQueryHelper, request: Request[A])
  extends WrappedRequest[A](request)