package services.url

import models.common.{FileIdent, HeimdallError, HeimdallRequest}
import com.evidence.service.common.logging.LazyLogging
import services.dredd.DreddClient
import services.sage.SageClient
import utils.HdlTtl

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import com.google.protobuf.duration.{Duration => ProtobufDuration}

import javax.inject.Inject

case class PresignedUrlRequest @Inject()(sage: SageClient, dredd: DreddClient)(implicit executionContext: ExecutionContext) extends LazyLogging{
  def getUrl[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration = HdlTtl.urlExpired): Future[URL] = {
    getUrlTest(file, request, ttl)
  }

  private def getUrlTest[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration = HdlTtl.urlExpired): Future[URL] = {
    for {
      sageRes <- getUrlfromSage(file, ttl)
      dreddRes <- getUrlfromDredd(file, request, ttl)
    } yield {
      compareClients(sageRes, dreddRes, file)
    }
  }

  /* Implement AB testing - compare Sage Client vs Dredd Client */
  private def compareClients(sageRes: Either[HeimdallError, URL], dreddRes: URL, file: FileIdent) : URL = {
    sageRes match {
      // Case 1: Sage fails, Dredd succeeds
      case Left(error) => {
        logger.debug("Fail to get url from Sage")("fileId" -> file.fileId, "error" -> error.errorCode)
        dreddRes
      }
      // Case 2: Both succeed
      case Right(url) => {
        val compared = url.equals(dreddRes)
        if (compared) {
          logger.debug("Sage successfully return the same url as Dredd")("url" -> url)
        } else {
          logger.debug("Sage returns a different Url from Dredd")("sageUrl" -> url, "dreddUrl" -> dreddRes)
        }
        dreddRes
      }
    }
  }

  // Internal get-url logic
  private def getUrlfromDredd[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration) : Future[URL] = {
    dredd.getUrl(file, request, ttl)
  }

  private def getUrlfromSage(file: FileIdent, ttl: Duration): Future[Either[HeimdallError, URL]] = {
    sage.getUrl(file, Some(convertTTL(ttl)))
  }

  // url expired duration in Sage Client requires protobuf duration type but ttl from request is FiniteDuration
  // This function mainly aims to convert these duration type
  private def convertTTL(ttl: Duration): ProtobufDuration = {
    ProtobufDuration(nanos = ttl.toSeconds.toInt)
  }
}
