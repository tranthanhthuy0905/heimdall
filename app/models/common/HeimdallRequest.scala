package models.common

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.thrift.v2.RequestInfo
import models.auth.{AuthorizationData, JWTWrapper}
import play.api.mvc.{Request, WrappedRequest}
import services.audit.AuditEvent
import services.komrade.PlaybackSettings

import java.util.UUID

case class HeimdallRequest[A](
  request: Request[A],
  authorizationData: AuthorizationData,
  watermark: String = "",
  playbackSettings: Option[PlaybackSettings] = None,
  auditEvent: Option[AuditEvent] = None)
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

  val requestInfo: RequestInfo = RequestInfo(
    correlationId = Some(UUID.randomUUID.toString),
    ipAddress = Some(clientIpAddress),
    callingService = Some("heimdall")
  )

  def jwt: String = this.authorizationData.jwt

  def parsedJwt: JWTWrapper = this.authorizationData.parsedJwt

  def subjectType: String = this.parsedJwt.subjectType

  def audienceId: String = this.parsedJwt.audienceId

  def subjectId: String = this.parsedJwt.subjectId

  def subjectDomain: Option[String] = this.parsedJwt.subjectDomain

  def cookie: String = this.authorizationData.token

  def streamingSessionToken: String = {
    request.queryString.getOrElse("streamingSessionToken", Seq()).headOption.getOrElse("")
  }

  def media: MediaIdent = request.attrs.get(MediaIdentAttr.Key).getOrElse(EmptyMediaIdent())

  def url: Option[String] = request.queryString.getOrElse("url", Seq()).headOption
}
