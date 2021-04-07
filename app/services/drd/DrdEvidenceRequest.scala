package services.drd

import java.util.UUID

import models.common.{FileIdent, HeimdallRequest}
import play.api.mvc.WrappedRequest

/**
  * DrdEvidenceRequest generates request URI digestible by Drd service.
  *
  * @return Generated URI as a string.
  */
case class DrdEvidenceRequest[A](userId: UUID, partnerId: UUID, evidenceId: UUID, request: HeimdallRequest[A])
    extends WrappedRequest[A](request)
