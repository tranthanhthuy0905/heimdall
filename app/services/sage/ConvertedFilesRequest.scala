package services.sage

import models.common.{FileIdent, HeimdallRequest}

import play.api.mvc.WrappedRequest

case class ConvertedFilesRequest[A](file: FileIdent, request: HeimdallRequest[A])
  extends WrappedRequest[A](request)
