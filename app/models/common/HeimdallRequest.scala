package models.common

import com.evidence.service.common.logging.LazyLogging
import models.auth.JWTWrapper
import play.api.mvc.{Request, WrappedRequest}
import scala.util.Success
import scala.util.Failure
import scala.util.Try

case class HeimdallRequest[A](request: Request[A], watermark: String = "")
    extends WrappedRequest[A](request)
    with LazyLogging {

  /** Gets the client IP address using remoteAddress API.
    * See https://www.playframework.com/documentation/2.6.0/api/java/play/mvc/Http.RequestHeader.html#remoteAddress--
    *
    * @return IP Address as a string.
    */
  def clientIpAddress: String = {
    request.remoteAddress
  }

  def jwt: String = {
    Try(request.attrs(AuthorizationAttr.Key)) match {
      case Failure(exception) =>
        logger.warn("notFoundAuthAttr")("request" -> request, "exception" -> exception)
        ""
      case Success(authHandler) =>
        authHandler.jwt
    }
  }

  def parsedJwt: Option[JWTWrapper] = {
    Try(request.attrs(AuthorizationAttr.Key)) match {
      case Failure(exception) =>
        logger.warn("notFoundAuthAttr")("request" -> request, "exception" -> exception)
        None
      case Success(authHandler) =>
        Some(authHandler.parsedJwt)
    }
  }

  def subjectType: String = {
    this.parsedJwt match {
      case Some(jwt) =>
        jwt.subjectType
      case None =>
        logger.warn("notFoundJWTWrapper")("request" -> request)
        ""
    }
  }

  def audienceId: String = {
    this.parsedJwt match {
      case Some(jwt) =>
        jwt.audienceId
      case None =>
        logger.warn("notFoundJWTWrapper")("request" -> request)
        ""
    }
  }

  def subjectId: String = {
    this.parsedJwt match {
      case Some(jwt) =>
        jwt.subjectId
      case None =>
        logger.warn("notFoundJWTWrapper")("request" -> request)
        ""
    }
  }

  def subjectDomain: Option[String] = {
    this.parsedJwt match {
      case Some(jwt) =>
        jwt.subjectDomain
      case None =>
        logger.warn("notFoundJWTWrapper")("request" -> request)
        None
    }
  }

  def cookie: String = {
    Try(request.attrs(AuthorizationAttr.Key)) match {
      case Failure(exception) =>
        logger.warn("notFoundAuthAttr")("request" -> request, "exception" -> exception)
        ""
      case Success(authHandler) =>
        authHandler.token
    }
  }

  def streamingSessionToken: String = {
    request.queryString.getOrElse("streamingSessionToken", Seq()).headOption.getOrElse("")
  }

  def media: MediaIdent = {
    Try(request.attrs(MediaIdentAttr.Key)) match {
      case Failure(exception) =>
        logger.warn("notFoundMediaAttr")("request" -> request, "exception" -> exception)
        EmptyMediaIdent()
      case Success(mediaHandler) =>
        mediaHandler
    }
  }

  def rtmApiVersion: Int = {
    if (request.path.startsWith("/v2")) 2 else 1
  }

  def apiPathPrefixForBuildingHlsManifest: String = {
    if (request.path.startsWith("/v2")) "/api/v2" else "/api/v1"
  }

}
