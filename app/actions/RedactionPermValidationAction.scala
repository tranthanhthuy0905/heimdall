package actions

import com.evidence.api.thrift.v1.{EntityDescriptor, TidEntities}
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.PermissionType
import play.api.mvc.{ActionFilter, Result, Results}
import services.drd.DrdEvidenceRequest
import services.pdp.PdpClient

import scala.concurrent.{ExecutionContext, Future}

case class RedactionPermValidationAction @Inject()(pdp: PdpClient)(
  implicit val executionContext: ExecutionContext
) extends ActionFilter[DrdEvidenceRequest]
    with LazyLogging {

  def filter[A](request: DrdEvidenceRequest[A]): Future[Option[Result]] = {
    val entities = List(
      EntityDescriptor(
        TidEntities.Evidence,
        request.evidenceId.toString,
        Option(request.evidencePartnerId.toString)
      )
    )

    // TODO: double-check PermissionType
    // https://taserintl.atlassian.net/browse/MSR-2624?focusedCommentId=512469
    authorize(request.request.jwt, entities, PermissionType.EvidenceView) map {
      case true =>
        None
      case _ =>
        logger.error("failedToEnforcePermissions")(
          "path"       -> request.path,
          "query"      -> request.queryString,
          "permission" -> PermissionType.EvidenceView
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
