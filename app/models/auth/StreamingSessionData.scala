package models.auth

import java.util.UUID

import com.evidence.api.thrift.v1.SessionTokenType
import com.evidence.service.common.crypto.Signature
import com.evidence.service.common.logging.LazyLogging
import javax.inject.{Inject, Singleton}
import services.sessions.SessionsClient

import scala.collection.SortedSet
import scala.concurrent.{ExecutionContext, Future}

trait StreamingSessionData {
  def createStreamingSessionToken(sessionCookie: String, fileIds: SortedSet[UUID]): Future[String]
}

@Singleton
class StreamingSessionDataImpl @Inject()(sessions: SessionsClient)(implicit val executionContext: ExecutionContext)
  extends StreamingSessionData with LazyLogging {

  def createStreamingSessionToken(sessionCookie: String, fileIds: SortedSet[UUID]): Future[String] = {
    val newSecret: String = generateSecret
    val newToken = generateStreamingSessionToken(newSecret, fileIds)
    sessions.updateSessionData(SessionTokenType.SessionCookie, sessionCookie, Map("secret" -> newSecret)) map { response =>
      logger.info("updatedSessionDataWithSecret")("response" -> response, "streamingSessionToken" -> newToken)
      newToken
    }
  }

  /**
    * Generates 128 bit secure random number for the streaming session secret.
    */
  private def generateSecret: String = {
    Signature.calculateRFC2104HMAC(UUID.randomUUID().toString, UUID.randomUUID().toString, Signature.HMAC_SHA256)
  }

  private def generateStreamingSessionToken(secret: String, fileIds: SortedSet[UUID]): String = {
    val allFileIds: String  = fileIds.fold("") {(z, i) => z.toString + i.toString}.toString
    Signature.calculateRFC2104HMAC(allFileIds, secret, Signature.HMAC_SHA256)
  }

}
