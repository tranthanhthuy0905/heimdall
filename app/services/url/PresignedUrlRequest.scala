package services.url

import models.common.{FileIdent, HeimdallError, HeimdallRequest}
import com.evidence.service.common.logging.LazyLogging
import services.dredd.DreddClient
import services.sage.SageClient
import utils.HdlTtl

import java.net.URL
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import com.google.protobuf.duration.{Duration => ProtobufDuration}

import javax.inject.Inject

case class PresignedUrlRequest @Inject()(sage: SageClient, dredd: DreddClient) extends LazyLogging{
  def getUrl[A](file: FileIdent, request: HeimdallRequest[A], ttl: Duration = HdlTtl.urlExpired): Future[URL] = {
    getUrlfromDredd(file, ttl, request)
  }

  // Internal get-url logic
  private def getUrlfromDredd[A](file: FileIdent, ttl: Duration, request: HeimdallRequest[A]) : Future[URL] = {
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
