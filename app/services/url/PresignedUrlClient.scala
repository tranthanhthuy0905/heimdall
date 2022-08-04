package services.url

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import models.common.{FileIdent, HeimdallRequest}
import play.api.cache.AsyncCacheApi
import services.dredd.DreddClient
import services.sage.SageClient
import utils.{HdlCache, HdlTtl}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

trait PresignedUrlClient {
  def getUrl[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration = HdlTtl.urlExpired): Future[URL]
}

@Singleton
case class PresignedUrlImpl @Inject()(sage: SageClient, dredd: DreddClient, cache: AsyncCacheApi)(
  implicit executionContext: ExecutionContext)
    extends PresignedUrlClient
    with LazyLogging
    with StrictStatsD {

  def getUrl[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration = HdlTtl.urlExpired): Future[URL] = {
    getUrlwithCache(file, request, ttl)
  }

  private def getUrlwithCache[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration): Future[URL] = {
    if (ttl >= HdlTtl.urlExpired) {
      val key = s"${file.partnerId}-${file.evidenceId}-${file.fileId}"
      statsd.increment("get_url_cache", "scope:total_request")
      cache.getOrElseUpdate[URL](key, HdlTtl.urlMemTTL) {
        HdlCache.PresignedUrl
          .get(key)
          .map { url =>
            Future.successful(url)
          }
          .getOrElse {
            val res = getUrlTest(file, request, ttl).map { url =>
              {
                HdlCache.PresignedUrl.set(key, url)
                statsd.increment("get_url_cache", "scope:miss")
                url
              }
            }
            res
          }
      }
    } else getUrlTest(file, request, ttl)
  }

  private def getUrlTest[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration): Future[URL] = {
    val sageResFuture  = getUrlfromSage(file, ttl)
    val dreddResFuture = getUrlfromDredd(file, request, ttl)

    // Always return dredd url response to keep performance of application the same
    for {
      dreddRes <- dreddResFuture
      _ <- sageResFuture recover {
        case e => dreddRes
      }
    } yield dreddRes
  }

  private def getUrlfromDredd[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration): Future[URL] = {
    executionTime[URL]("get_url", dredd.getUrl(file, request, ttl), false, "source:dredd")
  }

  private def getUrlfromSage(file: FileIdent, ttl: Duration): Future[URL] = {
    val baseTime = System.currentTimeMillis
    val future = sage.getUrl(file, ttl).flatMap {
      _.fold(
        l => {
          logger.error("noUrlResponseFromSage")("error" -> l, "fileId" -> file.fileId, "evidenceId" -> file.evidenceId)
          Future.failed(l)
        },
        url => Future.successful(url)
      )
    }
    executionTime[URL]("get_url", future, false, baseTime, "source:sage")
  }
}
