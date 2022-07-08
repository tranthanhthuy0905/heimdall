package services.url

import com.evidence.service.common.ServiceGlobal.statsd
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
import scala.util.{Failure}

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
    val sageResFuture = getUrlfromSage(file, ttl)
    val dreddResFuture = getUrlfromDredd(file, request, ttl)

    // Always return dredd url response to keep performance of application the same
    for {
      dreddRes <- dreddResFuture recoverWith {
            case e => {
              statsd.increment("presigned_url_error", "source:dredd")
              sageResFuture onComplete{
                case Failure(err) => {
                  statsd.increment("presigned_url_error", "source:sage")
                }
              }
              Future.failed(e)
            }
          }
      _ <- sageResFuture recover {
        case e => {
          // Create a metric graph to visualize the number of times Sage fails to return presigned-url
          statsd.increment("presigned_url_error", "source:sage")
          dreddRes
        }
      }
    } yield dreddRes
  }

  // Internal get-url logic
  private def getUrlfromDredd[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration) : Future[URL] = {
    dredd.getUrl(file, request, ttl)
  }

  private def getUrlfromSage(file: FileIdent, ttl: Duration): Future[URL] = {
    sage.getUrl(file, ttl).flatMap(_.fold(l => {
      val mes = l.message
      logger.debug("Sage fails to return URL ")("error" -> mes, "errorCode" -> l.errorCode)
      Future.failed( new Exception("Sage fails to return URL " + s"error=$mes"))
    },
      r => Future.successful(r))
    )
  }
}
