package actions

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common._
import play.api.mvc.{ActionRefiner, Results}
import services.dredd.DreddClient
import services.rti.RtiRequest
import utils.UUIDHelper

import scala.concurrent.ExecutionContext

case class RtiRequestAction @Inject()(
                                     dreddClient: DreddClient
                                     )(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[HeimdallRequest, RtiRequest] with LazyLogging with UUIDHelper {

  def refine[A](request: HeimdallRequest[A]) = {

    val result = for {
      file <- FutureEither.successful(getFileIdent(request))
      presignedUrl <- FutureEither(dreddClient.getUrl(file, request).map(Right(_)))
    } yield {
      RtiRequest(
        file.partnerId,
        file.evidenceId,
        file.fileId,
        presignedUrl,
        request.watermark,
        request)
    }
    result.future
  }

  private def getFileIdent[A](request: HeimdallRequest[A]): Either[Results.Status, FileIdent] = {
    (
      for {
        fileId <- getUuidValueByKey("file_id", request.queryString)
        evidenceId <- getUuidValueByKey("evidence_id", request.queryString)
        partnerId <- getUuidValueByKey("partner_id", request.queryString)
      } yield FileIdent(fileId, evidenceId, partnerId)
    ).toRight(Results.BadRequest)
  }

}
