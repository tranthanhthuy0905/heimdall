package services.url

import models.common.{FileIdent, HeimdallRequest}
import com.evidence.service.common.logging.LazyLogging
import services.dredd.DreddClient
import services.sage.SageClient
import utils.HdlTtl

import java.net.URL
import scala.concurrent.Future
import scala.concurrent.duration.Duration

case class PresignedUrlRequest(sage: SageClient, dredd: DreddClient) extends LazyLogging{
  def getUrl[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration = HdlTtl.urlExpired): Future[URL] = {
    getUrlfromDredd(file, ttl, request)
  }

  // Internal get-url logic
  private def getUrlfromDredd[A](file: FileIdent, ttl: Duration, request: HeimdallRequest[A]) : Future[URL] = {
    dredd.getUrl(file, request, ttl)
  }
}
