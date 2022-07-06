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
    getUrlfromDredd(file, request, ttl)
  }

  // Internal get-url logic
  private def getUrlfromDredd[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration) : Future[URL] = {
    dredd.getUrl(file, request, ttl)
  }
}
