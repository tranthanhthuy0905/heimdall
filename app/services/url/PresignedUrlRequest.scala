package services.url

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.statsd.StrictStatsD
import models.common.{FileIdent, HeimdallRequest}
import play.api.cache.AsyncCacheApi
import services.dredd.DreddClient
import services.sage.SageClient
import utils.{HdlCache, HdlTtl}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success}

case class PresignedUrlRequest @Inject()(sage: SageClient, dredd: DreddClient, cache: AsyncCacheApi)(implicit executionContext: ExecutionContext) extends LazyLogging with StrictStatsD {
  def getUrl[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration = HdlTtl.urlExpired): Future[URL] = {
    getUrlwithCache(file, request, ttl)
  }

  private def getUrlwithCache[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration): Future[URL] = {
    if (ttl >= HdlTtl.urlExpired) {
      val key = s"${file.partnerId}-${file.evidenceId}-${file.fileId}"
      var calledSide = ""
      var urlGetTime = System.currentTimeMillis()
      val urlCache = cache.getOrElseUpdate[URL](key, HdlTtl.urlMemTTL) {
        HdlCache.PresignedUrl
          .get(key)
          .map { url =>
            urlGetTime = System.currentTimeMillis()
            calledSide = "Redis"
            Future.successful(url)
          }
          .getOrElse {
            val res = getUrlTest(file, request, ttl).map { url =>
              {
                HdlCache.PresignedUrl.set(key, url)
                urlGetTime = System.currentTimeMillis()
                url
              }
            }
            calledSide = "new get"
            res
          }
      }
      urlCache.onComplete {
        case Success(url) =>
          if (calledSide == "new get") {
            statsd.increment("cache", "ratio:miss")
          } else {
            statsd.increment("cache", "ratio:hit")
          }
          // TODO: Should create Unit Test to test the correctness of Cache later. Current Test is only to check Log
          logger.info(s"Time to get the URL successfully from $calledSide")("time" -> urlGetTime)
          logger.info(s"Key-value pair successfully from $calledSide")("key" -> key, "value" -> url)
      }
      urlCache
    } else getUrlTest(file, request, ttl)
  }

  private def getUrlTest[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration): Future[URL] = {
    val sageResFuture = getUrlfromSage(file, ttl)
    val dreddResFuture = getUrlfromDredd(file, request, ttl)

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

  private def getUrlfromDredd[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration) : Future[URL] = {
    executionTime[URL]("service_call", dredd.getUrl(file, request, ttl), true, "source:dredd")
  }

  private def getUrlfromSage(file: FileIdent, ttl: Duration): Future[URL] = {
    val future = sage.getUrl(file, ttl).flatMap {_.fold(l => {
        logger.error("noUrlResponseFromSage")("error" -> l, "fileId" -> file.fileId, "evidenceId" -> file.evidenceId)
        Future.failed(l)
      }, url => Future.successful(url))
    }
    executionTime[URL]("service_call", future, true, "source:sage")
  }
}
