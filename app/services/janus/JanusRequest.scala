package services.janus

import java.util.UUID

import models.common.{FileIdent, HeimdallRequest}
import play.api.mvc.WrappedRequest

/**
  * JanusRequest generates request URI digestible by Janus.
  *
  * @return Generated URI as a string.
  */
case class JanusRequest[A](file: FileIdent, userId: UUID, request: HeimdallRequest[A])
    extends WrappedRequest[A](request)
