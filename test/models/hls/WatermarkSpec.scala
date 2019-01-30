package models.hls

import java.util.UUID
import java.util.UUID.randomUUID

import models.common.RtmQueryParams
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.komrade.KomradeClient
import utils.{DateTime, TestHelper}

import scala.collection.immutable.Map
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WatermarkSpec extends PlaySpec with MockitoSugar {

  class TestWatermark(komrade: KomradeClient) extends WatermarkImpl(komrade)

  val partnerId: UUID = randomUUID
  val userId: UUID = randomUUID
  val komradeMock: KomradeClient = mock[KomradeClient]
  when(komradeMock.getPartner(partnerId.toString)) thenReturn Future.successful(TestHelper.getPartner("test.domain"))
  when(komradeMock.getUser(partnerId.toString, userId.toString)) thenReturn Future.successful(TestHelper.getUser("bob"))
  val testWatermark = new TestWatermark(komradeMock)

  "Watermark helper" must {

    "generate label and add it to the query" in {
      val rtmQuery = TestHelper.getRtmQueryParams(partnerId, Map())
      val expectedLabel: String = s"Viewed by bob (test.domain) on ${DateTime.getUtcDate}"
      val expectedRtmQuery = RtmQueryParams(rtmQuery.media, rtmQuery.path, Map("label" -> expectedLabel))

      testWatermark.augmentQuery(rtmQuery, TestHelper.getJWTWrapper(partnerId.toString, userId.toString)) map {
        result => assert(result == expectedRtmQuery)
      }
    }

    "generate label and replace existing label" in {
      val rtmQuery = TestHelper.getRtmQueryParams(partnerId, Map("label" -> "SOME-LABEL"))
      val expectedLabel: String = s"Viewed by bob (test.domain) on ${DateTime.getUtcDate}"
      val expectedRtmQuery = RtmQueryParams(rtmQuery.media, rtmQuery.path, Map("label" -> expectedLabel))

      testWatermark.augmentQuery(rtmQuery, TestHelper.getJWTWrapper(partnerId.toString, userId.toString)) map {
        result => assert(result == expectedRtmQuery)
      }
    }
  }

}
