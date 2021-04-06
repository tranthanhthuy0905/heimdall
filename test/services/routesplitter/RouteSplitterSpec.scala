package services.routesplitter

import java.util.UUID

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import com.typesafe.config.Config
import org.mockito.Mockito._

class RouteSplitterSpec extends PlaySpec with MockitoSugar {

  "RouteSplitterProvider" should {

    "return a NoOpRouteSplitter instance when it is disabled" in {
      val mockConfig = mock[Config]
      when(mockConfig.getBoolean("service.route_splitter.enabled")) thenReturn false

      val routeSplitter = new RouteSplitterProvider(mockConfig).get()

      routeSplitter mustBe a[NoOpRouteSplitter]
    }

    "return a DefaultRouteSplitter instance when it is enable" in {
      val mockConfig = mock[Config]
      when(mockConfig.getBoolean("service.route_splitter.enabled")) thenReturn true

      val routeSplitter = new RouteSplitterProvider(mockConfig).get()

      routeSplitter mustBe a[DefaultRouteSplitter]
    }
  }

  "NoOpRouteSplitter" should {
    "always return the default version" in {
      val routeSplitter = NoOpRouteSplitter()
      routeSplitter.getApiVersion(UUID.randomUUID, 1) mustEqual 1
    }
  }

  "DefaultRouteSplitter" should {
    "distribute key evenly based on percentage threshold" in {
      val PERCENTAGE_THRESHOLD = 50
      val TOLERANCE = 5
      val TOTAL_REQUESTS = 10000
      val routeSplitter = DefaultRouteSplitter(PERCENTAGE_THRESHOLD)
      val targetCount = (1 to TOTAL_REQUESTS).map(_ => routeSplitter.getApiVersion(UUID.randomUUID, 1000))
        .filter(_ equals 1)
        .sum

      val targetPercentage = java.lang.Math.round((targetCount.toFloat / TOTAL_REQUESTS) * 100)

      ((PERCENTAGE_THRESHOLD + TOLERANCE - targetPercentage) >= 0) mustBe true
      ((PERCENTAGE_THRESHOLD - TOLERANCE - targetPercentage) <= 0) mustBe true
    }
  }
}
