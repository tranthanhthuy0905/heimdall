package models.auth

import java.util.UUID
import java.util.UUID.randomUUID

import com.evidence.service.common.crypto.Signature
import com.typesafe.config.Config
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.collection.SortedSet
import scala.concurrent.ExecutionContext.Implicits.global

class StreamingSessionDataSpec extends PlaySpec with MockitoSugar {

  class TestStreamingSessionData(config: Config) extends StreamingSessionDataImpl(config)

  val secret: String     = "testing-play-secret"
  val configMock: Config = mock[Config]
  when(configMock.getString("play.http.secret.key")) thenReturn secret
  val someToken: String = "some-token"
  val testSessionData   = new TestStreamingSessionData(configMock)

  "StreamingSessionData" must {

    "create token and verify against fixed value" in {
      val someUUID = UUID.fromString("12345678-1234-5678-1234-567812345678")
      val result =
        testSessionData.createToken(someToken, SortedSet[UUID](someUUID))
      result mustBe "XZW3EW3GkkKyNyfA7_Ywuzbfm6cQCSHRGq.nQDme74o-"
    }

    "create token" in {
      val someUUID = randomUUID
      val result =
        testSessionData.createToken(someToken, SortedSet[UUID](someUUID))
      val expected = Signature
        .calculateRFC2104HMAC(
          someToken + someUUID.toString,
          secret,
          Signature.HMAC_SHA256
        )
        .replaceAll("\\+", ".")
        .replaceAll("\\/", "_")
        .replaceAll("=", "-")

      result mustBe expected
    }

    "create token with some test axon token and empty file id set" in {
      val result = testSessionData.createToken(someToken, SortedSet[UUID]())
      val expected = Signature
        .calculateRFC2104HMAC(someToken + "", secret, Signature.HMAC_SHA256)
        .replaceAll("\\+", ".")
        .replaceAll("\\/", "_")
        .replaceAll("=", "-")
      result mustBe expected
    }

    "create token with empty axon token and empty file id set" in {
      val result = testSessionData.createToken("", SortedSet[UUID]())
      val expected = Signature
        .calculateRFC2104HMAC("", secret, Signature.HMAC_SHA256)
        .replaceAll("\\+", ".")
        .replaceAll("\\/", "_")
        .replaceAll("=", "-")
      result mustBe expected
    }

    "validate token" in {
      val someUUID = randomUUID
      val streamingSessionToken: String =
        Signature
          .calculateRFC2104HMAC(
            someToken + someUUID.toString,
            secret,
            Signature.HMAC_SHA256
          )
          .replaceAll("\\+", ".")
          .replaceAll("\\/", "_")
          .replaceAll("=", "-")
      val result = testSessionData.validateToken(
        streamingSessionToken,
        someToken,
        SortedSet[UUID](someUUID)
      )
      result mustBe true
    }
  }

}
