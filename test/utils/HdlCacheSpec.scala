package utils

import org.scalatestplus.play.PlaySpec
import com.github.sebruck.EmbeddedRedis
import org.scalatest.BeforeAndAfterAll
import redis.embedded.RedisServer
import java.net.URL

import com.evidence.service.komrade.thrift.{WatermarkPosition, WatermarkSetting}

import scala.concurrent.duration.{Duration, HOURS, MINUTES}

class HdlCacheSpec extends PlaySpec with EmbeddedRedis with BeforeAndAfterAll {
  var redis: Option[RedisServer] = None

  override def beforeAll(): Unit = {
    redis = Some(startRedis(42367))
  }

  override def afterAll(): Unit = {
    redis.foreach(stopRedis)
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
      HdlTtl.watermarkSettingsRedisTTL mustBe Duration(5, HOURS)
    }
  }
  "HdlCache" must {
    "be able to get/set username" in {
      noException should be thrownBy HdlCache.Username.set("key", "someuser")
      HdlCache.Username.get("key") mustBe Some("someuser")
      noException should be thrownBy HdlCache.Username.set("key", "someuser2")
      HdlCache.Username.get("key") mustBe Some("someuser2")
      HdlCache.Username.get("key1") mustBe None
    }
    "be able to get/set domain" in {
      noException should be thrownBy HdlCache.AgencyDomain.set("key", "somedomain")
      HdlCache.AgencyDomain.get("key") mustBe Some("somedomain")
      noException should be thrownBy HdlCache.AgencyDomain.set("key", "somedomain2")
      HdlCache.AgencyDomain.get("key") mustBe Some("somedomain2")
      HdlCache.AgencyDomain.get("key1") mustBe None
    }
    "be able to get/set url" in {
      val url = new URL("https://example.com")
      noException should be thrownBy HdlCache.PresignedUrl.set("key", url)
      HdlCache.PresignedUrl.get("key") mustBe Some(url)
      val url2 = new URL("https://example.com2")
      noException should be thrownBy HdlCache.PresignedUrl.set("key", url2)
      HdlCache.PresignedUrl.get("key") mustBe Some(url2)
      HdlCache.PresignedUrl.get("key1") mustBe None
    }
    "be able to get/set watermark settings" in {
      val setting = WatermarkSetting("someagency", WatermarkPosition.Center)
      noException should be thrownBy HdlCache.WatermarkSettings.set("key", setting)
      HdlCache.WatermarkSettings.get("key") mustBe Some(setting)

      val setting2 = WatermarkSetting("someagency", WatermarkPosition.BottomRight)
      noException should be thrownBy HdlCache.WatermarkSettings.set("key", setting2)
      HdlCache.WatermarkSettings.get("key") mustBe Some(setting2)
      HdlCache.WatermarkSettings.get("key1") mustBe None
    }
  }
}

class HdlCacheNoRedisSpec extends PlaySpec {
  "HdlCacheNoRedis" must {
    "survive without redis" in {
      (1 to 100).foreach(_ => noException should be thrownBy HdlCache.Username.set("key", "someuser"))
    }
  }
}
