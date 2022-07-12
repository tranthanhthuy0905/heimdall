package services.url

import models.common.{FileIdent, HeimdallRequest}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import services.dredd.DreddClient
import services.sage.SageClient
import utils.{HdlCache, HdlTtl}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import play.api.cache.AsyncCacheApi

import javax.inject.Inject
import scala.util.{Failure, Success}

case class PresignedUrlRequest @Inject()(sage: SageClient, dredd: DreddClient, cache: AsyncCacheApi)(implicit executionContext: ExecutionContext) extends LazyLogging with StrictStatsD{
  def getUrl[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration = HdlTtl.urlExpired): Future[URL] = {
    getUrlwithCache(file, request, ttl)
  }
  private def getUrlwithCache[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration): Future[URL] = {
    if (ttl >= HdlTtl.urlExpired) {
      val key = s"$file.partnerId-$file.evidenceId-$file.fileId"
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
                url
              }
            }
            res
          }
      }
    } else getUrlTest(file, request, ttl)
  }

  private def getUrlTest[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration): Future[URL] = {
    val startTime = System.currentTimeMillis()
    val sageResFuture = countGetUrlError(getUrlfromSage(file, ttl), "sage", startTime)
    val dreddResFuture = countGetUrlError(getUrlfromDredd(file, request, ttl), "dredd", startTime)

    // Always return dredd url response to keep performance of application the same
    for {
      dreddRes <- dreddResFuture
      _ <- sageResFuture recover {
        case e => dreddRes
      }
    } yield {
      dreddRes
    }
  }

  private def countGetUrlError(url: Future[URL], service: String, startTime: Long): Future[URL] = {
    url onComplete {
      case Success(_) =>
        statsd.time("service_call", System.currentTimeMillis() - startTime, s"service:$service", "status:success")
      case Failure(_) => {
        statsd.increment("presigned_url_error", s"source:$service")
        statsd.time("service_call", System.currentTimeMillis() - startTime, s"service:$service", "status:fail")
      }
    }
    url
  }

  // Internal get-url logic
  private def getUrlfromDredd[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration) : Future[URL] = {
    dredd.getUrl(file, request, ttl)
  }

  private def getUrlfromSage(file: FileIdent, ttl: Duration): Future[URL] = {
    sage.getUrl(file, ttl).flatMap(_.fold(l => Future.failed(l), r => Future.successful(r)))
  }
}
