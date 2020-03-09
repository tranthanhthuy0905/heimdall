package services.apidae

import java.util.UUID

import models.common.{FileIdent, HeimdallRequest}
import play.api.mvc.WrappedRequest

/**
  * ApidaeRequest generates request URI digestible by Apidae.
  *
  * @return Generated URI as a string.
  */
case class ApidaeRequest[A](file: FileIdent, userId: UUID, request: HeimdallRequest[A])
    extends WrappedRequest[A](request)
