package services.sage

import com.axon.sage.protos.common.common.Tid
import com.axon.sage.protos.query.evidence_message.{EvidenceFieldSelect, Evidence => SageEvidenceProto}
import com.axon.sage.protos.common.common.Tid.EntityType.{EVIDENCE, FILE}
import com.axon.sage.protos.query.argument.UrlTTL
import com.axon.sage.protos.query.file_message.{DownloadUrlFieldSelect, File, FileFieldSelect}
import com.axon.sage.protos.v1.query_service.{QueryServiceGrpc, ReadRequest}
import com.axon.sage.protos.v1.query_service.ReadRequest.{Criteria, Tids}
import com.axon.sage.protos.v1.evidence_video_service.{EvidenceVideoServiceGrpc, GetConvertedFilesRequest, GetConvertedFilesResponse, ConvertedFile}
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import com.evidence.service.common.logging.LoggingHelper
import scala.collection.immutable.Seq

import scala.concurrent.duration.Duration
import com.google.protobuf.duration.{Duration => ProtobufDuration}
import com.typesafe.config.Config

import java.util.concurrent.{Executor, TimeUnit}
import javax.inject.{Inject, Singleton}
import io.grpc.{CallCredentials, Metadata}
import models.common.{FileIdent, HeimdallError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import play.api.cache.AsyncCacheApi
import utils.{HdlCache, HdlTtl}

import java.net.URL

trait SageClient {
  def getEvidence(id: EvidenceId, query: QueryRequest): Future[Either[HeimdallError, Evidence]]
  def getEvidences(ids: Seq[EvidenceId], query: QueryRequest): Future[Either[HeimdallError, Seq[Evidence]]]
  def getEvidenceContentType(id: EvidenceId) : Future[Either[HeimdallError, String]]
  def getConvertedFiles(id: EvidenceId): Future[Either[HeimdallError, Seq[ConvertedFile]]]
  def getUrl(file: FileIdent,
             ttl: Duration): Future[Either[HeimdallError, URL]]
}

@Singleton
class SageClientImpl @Inject()(config: Config, cache: AsyncCacheApi)(implicit ex: ExecutionContext) extends SageClient
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
  val evidenceVideoService = EvidenceVideoServiceGrpc.stub(channel).withCallCredentials(callerSecret(credential(secret)))
  def queryServiceFn = queryService.withDeadlineAfter(queryDeadline, TimeUnit.SECONDS)
  def evidenceVideoServiceFn = evidenceVideoService.withDeadlineAfter(evVideoDeadline, TimeUnit.SECONDS)

  override def getEvidence(id: EvidenceId, query: QueryRequest): Future[Either[HeimdallError, Evidence]] = {
    for {
      res <- getEvidences(Seq(id), query)
      evidence <- Future.successful(res.map(evidences => evidences.find(evidence => evidence.evidenceId equals id.entityId)))
    } yield evidence.fold(l => Left(l), r => r.toRight(HeimdallError("evidence not found", HeimdallError.ErrorCode.NOT_FOUND)))
  }

  override def getEvidences(ids: Seq[EvidenceId], query: QueryRequest): Future[Either[HeimdallError, Seq[Evidence]]] = {
    val request = ReadRequest(
      path = query.path,
      context = Some(requestContext),
      criteria = Criteria.Tids(Tids(ids.map(id => Tid(
        entityType = EVIDENCE,
        entityId = id.entityId.toString,
        entityDomain = id.partnerId.toString
      ))))
    )

    for {
      res <- queryServiceFn.read(request).map(toEither)
    } yield res.map(entities => entities.map(entity => Evidence.fromSageProto(entity.entity.value.asInstanceOf[SageEvidenceProto])))
  }

  def getEvidenceContentType(id: EvidenceId) : Future[Either[HeimdallError, String]] = {
    def evidenceWithContentType = {
      val selection = EvidenceFieldSelect(
        partnerId = true,
        id = true,
        contentType = true
      ).namePaths().map(_.toProtoPath)

      getEvidence(id, QueryRequest(selection))
    }
    // use prefix to prevent somewhere there is somewhere using same entityId (fileId)
    val key = s"ect-${id.entityId}"

    // from play cache first
    cache.getOrElseUpdate[String](key, HdlTtl.evidenceContentTypeMemTTL) {
      // if not found then get from redis
      HdlCache.EvidenceContentType.get(key)
        .map(Future.successful)
        .getOrElse {
          // if not found then get from sage
          evidenceWithContentType.flatMap {
            case Left(l) => Future.failed(l)
            case Right(evidence) =>
              HdlCache.EvidenceContentType.set(key, evidence.contentType)
              Future.successful(evidence.contentType)
          }
        }
    }
      .map(Right(_))
      .recover {
        case heimdallErr: HeimdallError => Left(heimdallErr)
        case otherErr => Left(HeimdallError("internal server error", HeimdallError.ErrorCode.INTERNAL_SERVER_ERROR))
      }
  }

  override def getConvertedFiles(id: EvidenceId): Future[Either[HeimdallError, Seq[ConvertedFile]]] = {
    val request =  GetConvertedFilesRequest(
      context = Some(requestContext),
      partnerId = id.partnerId.toString,
      evidenceId = id.entityId.toString
    )

    evidenceVideoServiceFn.getConvertedFiles(request).map(toEither)
  }

  private def getFile(id: EvidenceId, query: QueryRequest) : Future[Either[HeimdallError, File]] = {
    val request = ReadRequest(
      path = query.path,
      context = Some(requestContext),
      criteria = Criteria.Tids(Tids(Seq(Tid(
        entityType = FILE,
        entityId = id.entityId.toString,
        entityDomain = id.partnerId.toString
      ))))
    )
    val fileRes = for {
      res <- queryServiceFn.read(request).map(toEither)
    } yield res.map(_.map(_.entity.value.asInstanceOf[File]))
    fileRes.map(_.map(_.headOption).flatMap(_.toRight(HeimdallError("File not found", HeimdallError.ErrorCode.NOT_FOUND))))
  }

  override def getUrl(file: FileIdent, ttl: Duration): Future[Either[HeimdallError, URL]] = {
    val fileReq = EvidenceId(file.fileId, file.partnerId)
    val urlArg = Some(UrlTTL(duration=Some(convertTTL(ttl))))
    val selection = FileFieldSelect(downloadUrl = Some(DownloadUrlFieldSelect(url=true, urlArgument = urlArg, expiredAt = true))).namePaths().map(_.toProtoPath)

    for {
      urlString <- getFile(fileReq, QueryRequest(selection)).map(manageDownloadUrl(_))
    } yield urlString.map(new URL(_))
  }

  private case class callerSecret(header: Metadata) extends CallCredentials {
    override def applyRequestMetadata(requestInfo: CallCredentials.RequestInfo, appExecutor: Executor, applier: CallCredentials.MetadataApplier): Unit = {
      appExecutor.execute(() => {
        applier.apply(header)
      })
    }
    override def thisUsesUnstableApi(): Unit = ()
  }

  def convertTTL(ttl: Duration): ProtobufDuration = {
    ProtobufDuration(seconds = ttl.toSeconds)
  }

  private def manageDownloadUrl(input: Either[HeimdallError, File]): Either[HeimdallError, String] = {
    val urlString = input.map(_.downloadUrl.map(_.url).map(_.trim).filter(_.nonEmpty))
    urlString.flatMap(_.toRight(HeimdallError("Presigned-url not found", HeimdallError.ErrorCode.NOT_FOUND)))
  }
}
