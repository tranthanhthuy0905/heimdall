package services.xpreport.playback

import models.common.{FileIdent, HeimdallRequest}
import play.api.mvc.WrappedRequest

case class XpReportRequest[A](file: FileIdent, request: HeimdallRequest[A])
  extends WrappedRequest[A](request)
