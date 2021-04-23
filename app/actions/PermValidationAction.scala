package actions

import com.evidence.api.thrift.v1.EntityDescriptor
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{HeimdallRequest, PermissionType}
import play.api.mvc.{ActionFilter, Result, Results}
import services.pdp.PdpClient

import scala.concurrent.{ExecutionContext, Future}

case class PermValidationActionBuilder @Inject()(pdp: PdpClient)(
  implicit val executionContext: ExecutionContext) {

  def build(permission: PermissionType.Value) = {
    PermValidationAction(permission)(pdp)(executionContext)
  }
}

case class PermValidationAction @Inject()(permission: PermissionType.Value)(pdp: PdpClient)(
  implicit val executionContext: ExecutionContext
) extends ActionFilter[HeimdallRequest]
    with LazyLogging {

  def filter[A](request: HeimdallRequest[A]): Future[Option[Result]] = {
    val entities = permission match {
      case PermissionType.Stream =>
        request.media.toFileEntityDescriptors
      case PermissionType.View =>
        request.media.toEvidenceEntityDescriptors
    }

    authorize(request.jwt, entities, permission) map {
      case true =>
        None
      case _ =>
        logger.error("failedToEnforcePermissions")(
          "streamingSessionToken" -> request.streamingSessionToken,
          "path"                  -> request.path,
          "query"                 -> request.queryString,
          "permission"            -> permission
        )
        Some(Results.Forbidden)
    }
  }

  private def authorize(
    jwt: String,
    entities: List[EntityDescriptor],
    permission: PermissionType.Value): Future[Boolean] = {
    val action = permission match {
      case PermissionType.Stream => PermissionType.FileStream
      case PermissionType.View   => PermissionType.EvidenceViewBasic
    }
    pdp.enforceBatch(jwt: String, entities: List[EntityDescriptor], action.toString)
  }
}
