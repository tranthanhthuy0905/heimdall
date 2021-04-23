package actions

import com.evidence.api.thrift.v1.{EntityDescriptor, TidEntities}
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{HeimdallRequest, PermissionType}
import play.api.mvc.{ActionFilter, Result, Results}
import services.pdp.PdpClient

import scala.concurrent.{ExecutionContext, Future}

case class PartnerPermValidationActionBuilder @Inject()(pdp: PdpClient)(
  implicit val executionContext: ExecutionContext) {

  def build(permission: PermissionType.Value) = {
    PartnerPermValidationAction(permission)(pdp)(executionContext)
  }
}

case class PartnerPermValidationAction @Inject()(permission: PermissionType.Value)(pdp: PdpClient)(
  implicit val executionContext: ExecutionContext
) extends ActionFilter[HeimdallRequest]
  with LazyLogging {

  def filter[A](request: HeimdallRequest[A]): Future[Option[Result]] = {
    val entities = List(
      EntityDescriptor(TidEntities.Partner, request.audienceId)
    )

    authorize(request.jwt, entities, permission).map {
      case true =>
        None
      case _ =>
        logger.error("failedToEnforcePermissions")(
          "path"                  -> request.path,
          "query"                 -> request.queryString,
          "subjectId"                -> request.subjectId,
          "permission"            -> permission
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
