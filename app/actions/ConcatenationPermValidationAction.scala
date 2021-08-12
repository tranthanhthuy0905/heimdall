package actions

import com.evidence.api.thrift.v1.{EntityDescriptor, TidEntities}
import com.evidence.service.common.logging.LazyLogging
import models.common.PermissionType
import play.api.mvc.{ActionFilter, Result, Results}
import services.apidae.ConcatenationRequest
import services.pdp.PdpClient

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ConcatenationPermValidationAction @Inject()(pdp: PdpClient)(
  implicit val executionContext: ExecutionContext
) extends ActionFilter[ConcatenationRequest]
    with LazyLogging {

  def filter[A](request: ConcatenationRequest[A]): Future[Option[Result]] = {
    val entities = request.files
      .map(f => EntityDescriptor(TidEntities.Evidence, f.evidenceId.toString, Option(request.partnerId.toString)))
      .toList

    authorize(request.request.jwt, entities, PermissionType.EvidenceEdit) map {
      case true =>
        None
      case _ =>
        logger.error("failedToEnforcePermissions")(
          "path"        -> request.path,
          "requestBody" -> request.body,
          "permission"  -> PermissionType.EvidenceEdit
        )
        Some(Results.Forbidden)
    }
  }

  private def authorize(
    jwt: String,
    entities: List[EntityDescriptor],
    permission: PermissionType.Value): Future[Boolean] = {
    pdp.enforceBatch(jwt: String, entities: List[EntityDescriptor], permission.toString)
  }
}
