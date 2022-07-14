package services.dredd

import com.evidence.api.thrift.v1.TidEntities
import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.finagle.FutureConverters._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.dredd.thrift._
import com.typesafe.config.Config
import models.common.{FileIdent, HeimdallRequest}
import utils.{HdlTtl, LatencyHelper}

import java.net.URL
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

trait DreddClient {
  def getUrl[A](file: FileIdent, request: HeimdallRequest[A]): Future[URL]

  def getUrl[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration): Future[URL]

  def getUrl[A](
    agencyId: UUID,
    evidenceId: UUID,
    fileId: UUID,
    request: HeimdallRequest[A],
    ttl: Duration = HdlTtl.urlExpired): Future[URL]

  def getUrlWithLatencyMetric[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration, metricName: String, tagList: String*): Future[URL]
}

@Singleton
class DreddClientImpl @Inject()(config: Config)(implicit ex: ExecutionContext)
    extends DreddClient
    with LazyLogging
    with LatencyHelper {

  private val client: DreddService.MethodPerEndpoint = {
    val env = FinagleClient.getEnvironment(config)
    val dest = FinagleClient.newThriftUrl(
      "com.evidence.service.dredd-service",
      env,
      "thrift"
    )
    val client = FinagleClient
      .newThriftClient()
      .build[DreddService.MethodPerEndpoint](dest)
    client
  }

  private val auth: Authorization = {
    val secret   = config.getString("edc.service.dredd.thrift_auth_secret")
    val authType = config.getString("edc.service.dredd.thrift_auth_type")
    Authorization(authType, secret)
  }

  override def getUrl[A](file: FileIdent, request: HeimdallRequest[A]): Future[URL] = {
    getUrl(file.partnerId, file.evidenceId, file.fileId, request)
  }

  override def getUrl[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration): Future[URL] = {
    getUrl(file.partnerId, file.evidenceId, file.fileId, request, ttl)
  }

  override def getUrl[A](
    partnerId: UUID,
    evidenceId: UUID,
    fileId: UUID,
    request: HeimdallRequest[A],
    ttl: Duration = HdlTtl.urlExpired): Future[URL] = {

    def extractUrlFromDreddResponse(r: PresignedUrlResponse): Future[URL] = {
      val u = for {
        presigned <- r.presignedUrl
        urlString <- presigned.url
        url = new URL(urlString)
      } yield Future(url)
      u.getOrElse {
        logger.error("noUrlInResponse")(
          "message"    -> "No URL contained in PresignedUrlResponse",
          "evidenceId" -> evidenceId,
          "partnerId"  -> partnerId
        )
        Future.failed(
          new Exception(
            "No URL contained in PresignedUrlResponse for " +
              s"evidenceId=$evidenceId partnerId=$partnerId"
          )
        )
      }
    }

    for {
      presignedUrlResponse <- getPresignedUrl(
        partnerId,
        evidenceId,
        fileId,
        request,
        ttl
      )
      url <- extractUrlFromDreddResponse(presignedUrlResponse)
    } yield url
  }

  private def getPresignedUrl[A](
    agencyId: UUID,
    evidenceId: UUID,
    fileId: UUID,
    input: HeimdallRequest[A],
    ttl: Duration
  ): Future[PresignedUrlResponse] = {
    val dreddUpdatedBy = Tid(
      TidEntities.valueOf(input.subjectType),
      Some(input.subjectId),
      input.subjectDomain
    )
    val requestContext =
      RequestContext(dreddUpdatedBy, Some(input.clientIpAddress))

    val request = PresignedUrlRequest(
      partnerId = agencyId.toString,
      evidenceId = evidenceId.toString,
      fileId = fileId.toString,
      expiresSecs = ttl.toSeconds.toInt,
      suppressAudit = Some(true)
    )
    client.getPresignedUrl2(auth, request, requestContext).toScalaFuture
  }

  override def getUrlWithLatencyMetric[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration, metricName: String, tagList: String*): Future[URL] = {
    val startTime = System.currentTimeMillis()
    val method = getUrl(file, request, ttl)
    val tags = tagList :+ "service:dredd"
    createLatencyMetric(method, metricName, startTime, tags: _*)
  }
}
