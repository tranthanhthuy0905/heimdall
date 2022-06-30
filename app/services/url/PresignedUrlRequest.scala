package services.url

import models.common.FileIdent
import com.evidence.service.common.logging.LazyLogging
import services.dredd.DreddClient
import services.sage.SageClient
import utils.HdlTtl

import java.net.URL
import scala.concurrent.Future
import scala.concurrent.duration.Duration

case class PresignedUrlRequest(sage: SageClient, dredd: DreddClient) extends LazyLogging{
  def getUrl(file: FileIdent, ttl: Duration = HdlTtl.urlExpired): Future[URL] = {

  }
  private def convertTTL(duration: Duration): Future[Duration] = {

  }
}
