package services.nino

import java.util.UUID

import com.evidence.api.thrift.v1.{EntityDescriptor, TidEntities}
import com.evidence.service.nino.api.thrift.AccessCheckResult
import org.scalatest.{FlatSpec, Matchers}

class PermissionHelperSpec extends FlatSpec with Matchers with PermissionsHelper {

  val evidenceId       = UUID.randomUUID
  val partnerId        = UUID.randomUUID
  val entityDescriptor = EntityDescriptor(TidEntities.Evidence, evidenceId.toString, Option(partnerId.toString))

  behavior of "PermissionHelper.ninoResultToAuthEntities"

  it should "return a list of AuthEntity" in {

    val accessResults = Seq(
      AccessCheckResult(target = entityDescriptor, granted = true, attributes = Set("id")),
      AccessCheckResult(target = entityDescriptor, granted = true, attributes = Set("id", "basic")),
      AccessCheckResult(target = entityDescriptor, granted = true, attributes = Set("id", "basic", "extended")),
      AccessCheckResult(target = entityDescriptor, granted = true, attributes = Set())
    )

    val authEntities = ninoResultToAuthEntities(accessResults)

    val scopes = authEntities.map(_.scope)
    scopes shouldBe Seq(idScope, minimalScope, fullScope, noneScope)
  }

  behavior of "PermissionHelper.allEntitiesGranted"

  it should "only passes when all entities are granted" in {
    val accessResults = Seq(
      AccessCheckResult(target = entityDescriptor, granted = true, attributes = Set("id")),
      AccessCheckResult(target = entityDescriptor, granted = true, attributes = Set("id", "basic"))
    )

    val result = allEntitiesGranted(ninoResultToAuthEntities(accessResults))
    result shouldBe true
  }

  it should "fails when there's entity which is not granted" in {
    val accessResults = Seq(
      AccessCheckResult(target = entityDescriptor, granted = true, attributes = Set("id")),
      AccessCheckResult(target = entityDescriptor, granted = false, attributes = Set("id", "basic"))
    )

    val result = allEntitiesGranted(ninoResultToAuthEntities(accessResults))
    result shouldBe false
  }

  behavior of "PermissionHelper.allEntitiesHaveScope"

  it should "only passes when all entities have the same scope" in {
    val accessResults = Seq(
      AccessCheckResult(target = entityDescriptor, granted = true, attributes = Set("id", "basic")),
      AccessCheckResult(target = entityDescriptor, granted = true, attributes = Set("id", "basic"))
    )

    val result = allEntitiesHaveScope(minimalScope)(ninoResultToAuthEntities(accessResults))
    result shouldBe true
  }

  it should "fails when entities have different scope" in {
    val accessResults = Seq(
      AccessCheckResult(target = entityDescriptor, granted = true, attributes = Set("id")),
      AccessCheckResult(target = entityDescriptor, granted = false, attributes = Set("id", "basic"))
    )

    val result = allEntitiesHaveScope(minimalScope)(ninoResultToAuthEntities(accessResults))
    result shouldBe false
  }

}
