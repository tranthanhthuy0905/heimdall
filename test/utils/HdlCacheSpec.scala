package utils

import org.scalatestplus.play.PlaySpec
import com.github.sebruck.EmbeddedRedis
import org.scalatest.BeforeAndAfterAll
import redis.embedded.RedisServer
import java.net.URL

import scala.concurrent.duration.{Duration, HOURS, MINUTES}

class HdlCacheSpec extends PlaySpec with EmbeddedRedis with BeforeAndAfterAll {
  var redis: RedisServer = null
  override def beforeAll() = {
    redis = startRedis(42367)
  }

  override def afterAll() = {
    stopRedis(redis)
  }

  "HdlTtl" must {
    "load correct ttl" in {
      HdlTtl.usernameMemTTL mustBe Duration(20, MINUTES)
      HdlTtl.usernameRedisTTL mustBe Duration(1, HOURS)
      HdlTtl.domainMemTTL mustBe Duration(1, HOURS)
      HdlTtl.domainRedisTTL mustBe Duration(5, HOURS)
      HdlTtl.urlExpired mustBe Duration(2, HOURS)
      HdlTtl.urlMemTTL mustBe Duration(20, MINUTES)
      HdlTtl.urlRedisTTL mustBe Duration(1, HOURS)
    }
  }
  "HdlCache" must {
    "be able to get/set/evict username" in {
      noException should be thrownBy HdlCache.Username.set("key", "someuser")
      HdlCache.Username.get("key") mustBe Some("someuser")
      noException should be thrownBy HdlCache.Username.evict("key")
      HdlCache.Username.get("key") mustBe None
    }
    "be able to get/set/evict domain" in {
      noException should be thrownBy HdlCache.AgencyDomain.set("key", "somedomain")
      HdlCache.AgencyDomain.get("key") mustBe Some("somedomain")
      noException should be thrownBy HdlCache.AgencyDomain.evict("key")
      HdlCache.AgencyDomain.get("key") mustBe None
    }
    "be able to get/set/evict url" in {
      val url = new URL("https://example.com")
      noException should be thrownBy HdlCache.PresignedUrl.set("key", url)
      HdlCache.PresignedUrl.get("key") mustBe Some(url)
      noException should be thrownBy HdlCache.PresignedUrl.evict("key")
      HdlCache.PresignedUrl.get("key") mustBe None
    }
  }
}
