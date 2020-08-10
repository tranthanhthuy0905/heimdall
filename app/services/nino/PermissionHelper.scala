package services.nino

import java.util.UUID
import scala.collection.Set

import com.evidence.service.nino.api.thrift.AccessCheckResult

case class AuthEntity(evidenceId: UUID, partnerId: Option[UUID], granted: Boolean, scope: String)

trait PermissionsHelper {

  // evidence view scope
  final val fullScope    = "full"
  final val minimalScope = "minimal"
  final val idScope      = "id"
  final val noneScope    = "none"

  def ninoResultToAuthEntities(authResults: Seq[AccessCheckResult]): Seq[AuthEntity] = {
    authResults.map { ar =>
      // domain always exists here, if it does not we have an error.
      assert(ar.target.domain.nonEmpty, "Intenal Server Error - AuthenticationMap domain should exist")

      val evidenceId     = UUID.fromString(ar.target.id)
      val maybePartnerId = ar.target.domain.map(UUID.fromString)
      val granted        = ar.granted
      val scope          = viewPermissionToScope(ar.attributes)

      AuthEntity(evidenceId, maybePartnerId, granted, scope)
    }
  }

  def allEntitiesGranted(authEntities: Seq[AuthEntity]): Boolean = authEntities.forall(_.granted)

  def allEntitiesHaveScope(scope: String)(authEntities: Seq[AuthEntity]): Boolean =
    allEntitiesGranted(authEntities) && authEntities.forall(_.scope equals scope)

  private def viewPermissionToScope(authScope: Set[String]): String = {
    if (authScope == Set("id", "basic", "extended")) {
      fullScope
    } else if (authScope == Set("id", "basic")) {
      minimalScope
    } else if (authScope == Set("id")) {
      idScope
    } else {
      noneScope
    }
  }
}
