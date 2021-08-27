package services.sage

import com.axon.sage.protos.common.common.{RequestContext, Tid}
import com.axon.sage.protos.query.evidence_message.{Evidence => SageEvidenceProto}
import com.axon.sage.protos.common.common.Tid.EntityType.{EVIDENCE}
import com.axon.sage.protos.v1.query_service.{ReadRequest, ReadResponse, QueryServiceGrpc}
import com.axon.sage.protos.v1.query_service.ReadRequest.{Criteria, Tids}
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import com.evidence.service.common.logging.LoggingHelper
import com.typesafe.config.Config
import java.util.UUID
import java.util.concurrent.{Executor, TimeUnit}
import javax.inject.{Inject, Singleton}
import io.grpc.{Metadata, CallCredentials}
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try
import scala.util.control.NonFatal

trait SageClient {
  def getEvidence(id: EvidenceId, query: QueryRequest): Future[Either[Throwable, Evidence]]
  def getEvidences(ids: Seq[EvidenceId], query: QueryRequest): Future[Either[Throwable, Seq[Evidence]]]
}

@Singleton
class SageClientImpl @Inject()(config: Config)(implicit ex: ExecutionContext) extends SageClient
  with SageClientHelper
  with StrictStatsD
  with LoggingHelper {

  val sageConfig      = config.getConfig("edc.service.sage")
  val secret          = sageConfig.getString("secret")
  val host            = sageConfig.getString("host")
  val port            = sageConfig.getInt("port")
  val queryDeadline   = Try(sageConfig.getLong("deadline.query")).getOrElse(10L)
  val evVideoDeadline = Try(sageConfig.getLong("deadline.evidenceVideo")).getOrElse(10L)
  val ssl             = Try(sageConfig.getBoolean("ssl")).getOrElse(true)
  val keepAliveConfig = Try(sageConfig.getConfig("keepalive")).getOrElse(null)

  val channel         = buildChannel(
    host, port, ssl, keepAliveConfig, 
    RetryConfig(
      methods = Seq(
        RetryMethod("Read", "QueryService"),
      ),
      protoPackage = "com.axon.sage.protos.v1"
    )
  )

  val queryService = QueryServiceGrpc.stub(channel).withCallCredentials(callerSecret(credential(secret)))
  def queryServiceFn = queryService.withDeadlineAfter(queryDeadline, TimeUnit.SECONDS)

  override def getEvidence(id: EvidenceId, query: QueryRequest): Future[Either[Throwable, Evidence]] = {
    getEvidences(Seq(id), query).map {
      case Left(err) => Left(err)
      case Right(evidenceSeq) =>
        if (evidenceSeq.length == 0) Left(new Exception("Evidence not found")) else Right(evidenceSeq(0))
    }
  }

  override def getEvidences(ids: Seq[EvidenceId], query: QueryRequest): Future[Either[Throwable, Seq[Evidence]]] = {
    val request = ReadRequest(
      path = query.path,
      context = Some(requestContext),
      criteria = Criteria.Tids(Tids(ids.map(id => Tid(
        entityType = EVIDENCE,
        entityId = id.entityId.toString,
        entityDomain = id.partnerId.toString
      ))))
    )

    queryServiceFn.read(request).map {
      case ReadResponse(Some(err), _, _) => Left(new Exception("Sage query error: " + err))
      case resp => Right(resp.entities.map(evidence => Evidence.fromSageProto(evidence.entity.value.asInstanceOf[SageEvidenceProto])))
    }
    .recover {
      case NonFatal(ex) => Left(ex)
    }
  }

  private def requestContext = RequestContext(
    correlationId = UUID.randomUUID.toString,
    callingService = "heimdall"
  )

  private def credential(secret: String): Metadata = {
    val header = new Metadata()
    header.put(Metadata.Key.of("secret", Metadata.ASCII_STRING_MARSHALLER), secret)
    header
  }

  private case class callerSecret(header: Metadata) extends CallCredentials {
    override def applyRequestMetadata(requestInfo: CallCredentials.RequestInfo, appExecutor: Executor, applier: CallCredentials.MetadataApplier): Unit = {
      appExecutor.execute(() => {
        applier.apply(header)
      })
    }
    override def thisUsesUnstableApi(): Unit = ()
  }
}
