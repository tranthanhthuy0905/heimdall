package models.auth

import java.util.UUID

import com.evidence.service.common.crypto.Signature
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}

import scala.collection.SortedSet
import scala.concurrent.ExecutionContext

trait StreamingSessionData {
  def createToken(axonSessionToken: String, fileIds: SortedSet[UUID]): String
  def validateToken(streamingSessionToken: String, axonSessionToken: String, fileIds: SortedSet[UUID]): Boolean
}

@Singleton
class StreamingSessionDataImpl @Inject()(config: Config)(
  implicit val executionContext: ExecutionContext
) extends StreamingSessionData
    with LazyLogging {

  private val secret: String = config.getString("play.http.secret.key")

  /**
    * createToken returns a Base64 encoded token, however it replaces the "+", "/", "=" characters
    * with ".", "_", "-" respectively to make the token URL-safe.
    */
  def createToken(axonSessionToken: String, fileIds: SortedSet[UUID]): String = {
    val allFileIds: String = fileIds
      .fold("") { (z, i) =>
        z.toString + i.toString
      }
      .toString
    val token = Signature.calculateRFC2104HMAC(
      axonSessionToken + allFileIds,
      secret,
      Signature.HMAC_SHA256
    )
    token
      .replaceAll("\\+", ".")
      .replaceAll("\\/", "_")
      .replaceAll("=", "-")
  }

  def validateToken(streamingSessionToken: String, axonSessionToken: String, fileIds: SortedSet[UUID]): Boolean = {
    if (fileIds.isEmpty || axonSessionToken.isEmpty || streamingSessionToken.isEmpty) {
      return false
    }
    val expectedToken = createToken(axonSessionToken, fileIds)
    streamingSessionToken.equals(expectedToken)
  }
}
