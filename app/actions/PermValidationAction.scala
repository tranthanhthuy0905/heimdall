package actions

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{HeimdallRequest, PermissionType}
import play.api.mvc.{ActionFilter, Result, Results}
import services.nino.NinoClient
import scala.concurrent.{ExecutionContext, Future}


case class PermValidationActionBuilder @Inject()(nino: NinoClient)(implicit val executionContext: ExecutionContext) {
  def build(permission: PermissionType.Value) = {
    PermValidationAction(permission)(nino)(executionContext)
  }
}

case class PermValidationAction @Inject()(permission: PermissionType.Value)(nino: NinoClient)
                                    (implicit val executionContext: ExecutionContext)
    extends ActionFilter[HeimdallRequest]
    with LazyLogging {
  def filter[A](request: HeimdallRequest[A]): Future[Option[Result]] = {
    val entities = permission match {
      case PermissionType.Stream =>
        request.media.toFileEntityDescriptors
      case PermissionType.View =>
        request.media.toEvidenceEntityDescriptors
    }
    nino.enforce(request.jwt, entities, permission) map {
      case true =>
        None
      case _ =>
        logger.error("failedToEnforcePermissions")(
          "streamingSessionToken" -> request.streamingSessionToken,
          "path" -> request.path,
          "query" -> request.queryString,
          "cookie" -> request.cookie,
          "permission" -> permission
        )
        Some(Results.Forbidden)
    }
  }
}
