package services.audit

import com.evidence.service.audit._
import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.finagle.FutureConverters._
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import utils.HdlCache

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait AuditClient {
  def recordEndSuccess(event: AuditEvent): Future[Either[Throwable, String]]
  def recordEndSuccess(event: List[AuditEvent]): Future[Either[Throwable, List[String]]]
  def recordEndSuccessDebounce(event: List[AuditEvent]): Future[Either[Throwable, List[String]]]
}

@Singleton
class AuditClientImpl @Inject()(config: Config)(implicit ex: ExecutionContext) extends AuditClient with LazyLogging {

  private val client: AuditService.MethodPerEndpoint = {
    val env    = FinagleClient.getEnvironment(config)
    val dest   = FinagleClient.newThriftUrl("com.evidence.service.audit-service", env, "thrift")
    val client = FinagleClient.newThriftClient().build[AuditService.MethodPerEndpoint](dest)
    client
  }

  private val auth: Authorization = {
    val secret   = config.getString("edc.service.audit.thrift_auth_secret")
    val authType = config.getString("edc.service.audit.thrift_auth_type")
    Authorization(authType, "N/A", "N/A", secret)
  }

  private def doRecordAuditDebounce(event: AuditEvent): Future[String] = {
    val key = event.sha256Hash()
    if (HdlCache.AuditDebounce.get(key).getOrElse(false)) {
      doRecordAudit(event).map(res => {
        HdlCache.AuditDebounce.set(key, value = true)
        res
      })
    } else {
      Future.successful("debounced")
    }
  }

  private def doRecordAudit(event: AuditEvent): Future[String] =
    client
      .recordEventEndWithSuccess(
        auth,
        event.eventTypeUuid,
        event.targetTid,
        event.updatedByTid,
        event.toJsonString
      )
      .toScalaFuture

  override def recordEndSuccess(events: List[AuditEvent]): Future[Either[Throwable, List[String]]] = {
    Future
      .traverse(events)(doRecordAudit)
      .transformWith(value => Future.successful(value.toEither))
  }

  override def recordEndSuccessDebounce(events: List[AuditEvent]): Future[Either[Throwable, List[String]]] = {
    Future
      .traverse(events)(doRecordAuditDebounce)
      .transformWith(value => Future.successful(value.toEither))
  }

  override def recordEndSuccess(event: AuditEvent): Future[Either[Throwable, String]] = {
    doRecordAudit(event)
      .transformWith(value => Future.successful(value.toEither))
  }
}
