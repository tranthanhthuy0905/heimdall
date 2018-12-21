package models.auth

import java.util.UUID

import com.evidence.service.common.crypto.Signature
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import services.sessions.SessionsClient

import scala.collection.SortedSet
import scala.concurrent.ExecutionContext

trait StreamingSessionData {
  def createStreamingSessionToken(axonSessionToken: String, fileIds: SortedSet[UUID]): String
  def validateStreamingSessionToken(streamingSessionToken: String, axonSessionToken: String, fileIds: SortedSet[UUID]): Boolean
}

@Singleton
class StreamingSessionDataImpl @Inject()(sessions: SessionsClient, config: Config)(implicit val executionContext: ExecutionContext)
  extends StreamingSessionData with LazyLogging {

  private val secret: String = config.getString("service.secret")

  def createStreamingSessionToken(axonSessionToken: String, fileIds: SortedSet[UUID]): String = {
    val allFileIds: String  = fileIds.fold("") {(z, i) => z.toString + i.toString}.toString
    Signature.calculateRFC2104HMAC(axonSessionToken + allFileIds, secret, Signature.HMAC_SHA256)
  }

  def validateStreamingSessionToken(streamingSessionToken: String, axonSessionToken: String, fileIds: SortedSet[UUID]): Boolean = {
    val expectedToken = createStreamingSessionToken(axonSessionToken, fileIds)
    streamingSessionToken.equals(expectedToken)
  }
}
