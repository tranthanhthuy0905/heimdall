package services.url

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import models.common.FileIdent
import play.api.cache.AsyncCacheApi
import services.sage.SageClient
import utils.{HdlCache, HdlTtl}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

trait PresignedUrlClient {
  def getUrl(file: FileIdent, ttl: Duration = HdlTtl.urlExpired): Future[URL]
}

@Singleton
case class PresignedUrlImpl @Inject()(sage: SageClient, cache: AsyncCacheApi)(
  implicit executionContext: ExecutionContext)
    extends PresignedUrlClient
    with LazyLogging
    with StrictStatsD {

  def getUrl(file: FileIdent, ttl: Duration = HdlTtl.urlExpired): Future[URL] = {
    getUrlwithCache(file, ttl)
  }

  private def getUrlwithCache(file: FileIdent, ttl: Duration): Future[URL] = {
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
            val res = getUrlfromSage(file, ttl).map { url =>
              {
                HdlCache.PresignedUrl.set(key, url)
                statsd.increment("get_url_cache", "scope:miss")
                url
              }
            }
            res
          }
      }
    } else getUrlfromSage(file, ttl)
  }

  private def getUrlfromSage(file: FileIdent, ttl: Duration): Future[URL] = {
    val baseTime = System.currentTimeMillis
    val future = sage.getUrl(file, ttl).flatMap {
      _.fold(
        l => {
          logger.error("noUrlResponseFromSage")("message" -> l.message, "errorCode" -> l.errorCode, "fileId" -> file.fileId, "evidenceId" -> file.evidenceId, "partnerId" -> file.partnerId)
          Future.failed(l)
        },
        url => Future.successful(url)
      )
    }
    executionTime[URL]("get_url", future, false, baseTime, "source:sage")
  }
}
