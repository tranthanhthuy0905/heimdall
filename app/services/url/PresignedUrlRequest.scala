package services.url

import models.common.{FileIdent, HeimdallRequest}
import com.evidence.service.common.logging.LazyLogging
import services.dredd.DreddClient
import services.sage.SageClient
import utils.HdlTtl

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

import javax.inject.Inject

case class PresignedUrlRequest @Inject()(sage: SageClient, dredd: DreddClient)(implicit executionContext: ExecutionContext) extends LazyLogging{
  def getUrl[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration = HdlTtl.urlExpired): Future[URL] = {
    getUrlTest(file, request, ttl)
  }

  private def getUrlTest[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration): Future[URL] = {
    val sageResFuture = getUrlfromSage(file, ttl)
    val dreddResFuture = getUrlfromDredd(file, request, ttl)

    // Always return dredd url response to keep performance of application the same
    for {
      sageRes <- sageResFuture
      dreddRes <- dreddResFuture
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
