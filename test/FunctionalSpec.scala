import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._

/**
 * Functional tests start a Play application internally, available
 * as `app`.
 */
class FunctionalSpec extends PlaySpec with GuiceOneAppPerSuite {

  "Routes" must {

    "send 404 on a non-existing route" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

    "send 400 on a HLS bad request - missing query parameters" in  {
      route(app, FakeRequest(GET, "/media/hls/segment")).map(status(_)) mustBe Some(BAD_REQUEST)
    }

    "send 400 on a probe bad request - missing query parameters" in  {
      route(app, FakeRequest(GET, "/media/start")).map(status(_)) mustBe Some(BAD_REQUEST)
    }

    "send 200 on a good request" in  {
      route(app, FakeRequest(GET, "/media/alive")).map(status(_)) mustBe Some(OK)
    }

  }
}
