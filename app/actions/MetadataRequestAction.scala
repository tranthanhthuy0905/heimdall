package actions

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Result, Results}
import services.metadata.MetadataRequest
import utils.UUIDHelper
import scala.collection.immutable.Seq

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class MetadataRequestAction @Inject()()(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, MetadataRequest]
    with LazyLogging
    with UUIDHelper {

  def refine[A](request: HeimdallRequest[A]): Future[Either[Result, MetadataRequest[A]]] = {
    val result = for {
      file            <- request.media.headOption.toRight(Results.BadRequest)
      snapshotVersion <- request.queryString.getOrElse("version", Seq()).headOption.toRight(Results.BadRequest)
    } yield MetadataRequest(file, snapshotVersion, request)
    Future.successful(result)
  }
}
