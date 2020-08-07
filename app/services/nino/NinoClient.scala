package services.nino

import com.evidence.api.thrift.v1.EntityDescriptor
import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.finagle.FutureConverters._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import com.evidence.service.nino.api.thrift.{BatchAccessCheckRequest, Nino}
import com.evidence.service.thrift.v2.{Authorization, RequestInfo}
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.common.PermissionType

import scala.concurrent.{ExecutionContext, Future}

trait NinoClient {
  def enforce(jwtString: String, entities: List[EntityDescriptor], action: PermissionType.Value): Future[Boolean]
}

@Singleton
class NinoClientImpl @Inject()(config: Config)(implicit ex: ExecutionContext)
  extends NinoClient
    with LazyLogging
    with StrictStatsD
{

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

  def enforce(jwtString: String, entities: List[EntityDescriptor], action: PermissionType.Value): Future[Boolean] = {
    val request = BatchAccessCheckRequest(jwtString, entities, action.toString)
    executionTime(
      aspect = "NinoClient.enforceBatch",
      future =
        client
          .enforceBatch(auth, request, RequestInfo())
          .map { seqOfAccessResults =>
            seqOfAccessResults.filter(!_.granted).isEmpty
          }
          .toScalaFuture
    )
  }
}
