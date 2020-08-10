package services.nino

import java.util.UUID

import com.evidence.api.thrift.v1.EntityDescriptor
import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.finagle.FutureConverters._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import com.evidence.service.nino.api.thrift.{AccessCheckResult, BatchAccessCheckRequest, Nino}
import com.evidence.service.thrift.v2.{Authorization, RequestInfo}
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.common.PermissionType

import scala.concurrent.{ExecutionContext, Future}

trait NinoClient {
  def enforceStreamPermission(jwtString: String, entities: Seq[EntityDescriptor]): Future[Boolean]
  def enforceViewPermission(jwtString: String, entities: Seq[EntityDescriptor]): Future[Boolean]
}

@Singleton
class NinoClientImpl @Inject()(config: Config)(implicit ex: ExecutionContext)
    extends NinoClient
    with PermissionsHelper
    with LazyLogging
    with StrictStatsD {
  private val client: Nino.MethodPerEndpoint = {
    val env = FinagleClient.getEnvironment(config)
    val dest = FinagleClient.newThriftUrl(
      "com.evidence.service.nino-service",
      env,
      "thrift"
    )
    val client =
      FinagleClient.newThriftClient().build[Nino.MethodPerEndpoint](dest)
    client
  }

  private val auth: Authorization = {
    val secret   = config.getString("edc.service.nino.thrift_auth_secret")
    val authType = config.getString("edc.service.nino.thrift_auth_type")
    Authorization(Option(authType), Option(secret))
  }

  private def enforce(
    jwtString: String,
    entities: Seq[EntityDescriptor],
    action: PermissionType.Value): Future[Seq[AccessCheckResult]] = {
    val request = BatchAccessCheckRequest(jwtString, entities, action.toString)
    executionTime(
      aspect = "NinoClient.enforceBatch",
      future = client
        .enforceBatch(
          auth,
          request,
          requestInfo = RequestInfo(
            correlationId = Some(UUID.randomUUID.toString),
            callingService = Some("heimdall")
          ))
        .toScalaFuture
    )
  }

  override def enforceStreamPermission(jwtString: String, entities: Seq[EntityDescriptor]): Future[Boolean] =
    enforce(jwtString, entities, PermissionType.Stream)
      .map(ninoResultToAuthEntities)
      .map(allEntitiesGranted)

  override def enforceViewPermission(jwtString: String, entities: Seq[EntityDescriptor]): Future[Boolean] =
    enforce(jwtString, entities, PermissionType.View)
      .map(ninoResultToAuthEntities)
      .map(allEntitiesHaveScope(fullScope))
}
