package services.audit

import com.evidence.service.audit._
import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.finagle.FutureConverters._
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

trait AuditClient {
  def recordEndSuccess(event: AuditEvent): Future[String]
  def recordEndSuccess(event: List[AuditEvent]): Future[List[String]]
}

@Singleton
class AuditClientImpl @Inject()(config: Config)(implicit ex: ExecutionContext) extends AuditClient with LazyLogging {

  private val client: AuditService.MethodPerEndpoint = {
    val env = FinagleClient.getEnvironment(config)
    val dest = FinagleClient.newThriftUrl("com.evidence.service.audit-service", env, "thrift")
    val client = FinagleClient.newThriftClient().build[AuditService.MethodPerEndpoint](dest)
    client
  }

  private val auth: Authorization = {
    val secret = config.getString("edc.service.audit.thrift_auth_secret")
    val authType = config.getString("edc.service.audit.thrift_auth_type")
    Authorization(authType, "N/A", "N/A", secret)
  }

  override def recordEndSuccess(events: List[AuditEvent]): Future[List[String]] = {
    Future.traverse(events)(recordEndSuccess)
  }

  override def recordEndSuccess(event: AuditEvent): Future[String] = {
    val eventJson = event.toJsonString
    logger.info("auditRecordEventEndWithSuccess")(
      "eventTypeUuid" -> event.eventTypeUuid,
      "targetTidEntity" -> event.targetTid.entity,
      "targetTidId" -> event.targetTid.id,
      "targetTidDomain" -> event.targetTid.domain,
      "updatedByTidEntity" -> event.updatedByTid.entity,
      "updatedByTidId" -> event.updatedByTid.id,
      "updatedByTidDomain" -> event.updatedByTid.domain,
      "eventJson" -> eventJson
    )

    client.recordEventEndWithSuccess(
      auth,
      event.eventTypeUuid,
      event.targetTid,
      event.updatedByTid,
      eventJson
    ).toScalaFuture
  }

}
