import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._

/**
 * Functional tests start a Play application internally, available
 * as `app`.
 */
class FunctionalSpec extends PlaySpec with GuiceOneAppPerSuite {

  "Routes" should {

    "send 401 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(UNAUTHORIZED)
    }

    "send 401 on an unauthorized request" in  {
      route(app, FakeRequest(GET, "/media/hls/segment")).map(status(_)) mustBe Some(UNAUTHORIZED)
    }

    "send 200 on a good request" in  {
      route(app, FakeRequest(GET, "/media/alive")).map(status(_)) mustBe Some(OK)
    }

  }
}
