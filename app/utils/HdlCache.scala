package utils

import java.net.URL
import java.util.concurrent.TimeUnit
import com.evidence.service.common.cache.{Cache, CacheConfig}
import com.evidence.service.common.config.Configuration
import com.evidence.service.komrade.thrift.WatermarkSetting

import java.time.Instant
import scala.concurrent.duration.{Duration, FiniteDuration}

trait HdlCache[K, T] {
  def get(key: K): Option[T]
  def set(key: K, value: T): Unit
  def evict(key: K): Unit
}

case class cacheValue[K](
  value: K,
  expired: Instant
)

case object cacheValue {

  def apply[K](value: K, ttl: Duration): cacheValue[K] = cacheValue(
    value = value,
    expired = Instant.now().plusNanos(ttl.toNanos),
  )
}

case object HdlTtl {
  private val config = Configuration.load()

  val usernameRedisTTL: FiniteDuration =
    Duration(config.getDuration("service.cache.username.redis-ttl", TimeUnit.HOURS), TimeUnit.HOURS)

  val usernameMemTTL: FiniteDuration =
    Duration(config.getDuration("service.cache.username.mem-ttl", TimeUnit.MINUTES), TimeUnit.MINUTES)

  val domainRedisTTL: FiniteDuration =
    Duration(config.getDuration("service.cache.domain.redis-ttl", TimeUnit.HOURS), TimeUnit.HOURS)

  val domainMemTTL: FiniteDuration =
    Duration(config.getDuration("service.cache.domain.mem-ttl", TimeUnit.HOURS), TimeUnit.HOURS)

  val urlExpired: FiniteDuration =
    Duration(config.getDuration("service.cache.url.expired", TimeUnit.HOURS), TimeUnit.HOURS)

  val urlRedisTTL: FiniteDuration =
    Duration(config.getDuration("service.cache.url.redis-ttl", TimeUnit.HOURS), TimeUnit.HOURS)

  val urlMemTTL: FiniteDuration =
    Duration(config.getDuration("service.cache.url.mem-ttl", TimeUnit.MINUTES), TimeUnit.MINUTES)

  val watermarkSettingsRedisTTL: FiniteDuration =
    Duration(config.getDuration("service.cache.watermark-settings.redis-ttl", TimeUnit.HOURS), TimeUnit.HOURS)
}

case object HdlCache {
  private def hdlKey(key: String) = "hdl-v1-" + key
  private def getValue[K](someValue: Option[cacheValue[K]]): Option[K] = {
    someValue.flatMap(
      value =>
        if (Instant.now().isBefore(value.expired)) {
          Some(value.value)
        } else None
    )
  }

  case object Username extends HdlCache[String, String] {
    private val cache = new Cache[cacheValue[String]](CacheConfig(ttl = Some(HdlTtl.usernameRedisTTL)))

    override def get(key: String): Option[String] =
      getValue(cache.getSync(hdlKey(key), "Username", refreshTTL = false))

    override def set(key: String, value: String): Unit =
      cache.setSync(hdlKey(key), "Username", cacheValue(value, HdlTtl.usernameRedisTTL))

    override def evict(key: String): Unit = cache.deleteSync(hdlKey(key))
  }

  case object AgencyDomain extends HdlCache[String, String] {
    private val cache = new Cache[cacheValue[String]](CacheConfig(ttl = Some(HdlTtl.domainRedisTTL)))

    override def get(key: String): Option[String] =
      getValue(cache.getSync(hdlKey(key), "AgencyDomain", refreshTTL = false))

    override def set(key: String, value: String): Unit =
      cache.setSync(hdlKey(key), "AgencyDomain", cacheValue(value, HdlTtl.domainRedisTTL))

    override def evict(key: String): Unit = cache.deleteSync(hdlKey(key))
  }

  case object PresignedUrl extends HdlCache[String, URL] {
    private val cache = new Cache[cacheValue[String]](CacheConfig(ttl = Some(HdlTtl.urlRedisTTL)))

    override def get(key: String): Option[URL] =
      getValue(cache.getSync(hdlKey(key), "PresignedUrl", refreshTTL = false)).map(url => new URL(url))

    override def set(key: String, value: URL): Unit =
      cache.setSync(hdlKey(key), "PresignedUrl", cacheValue(value.toString, HdlTtl.urlRedisTTL))

    override def evict(key: String): Unit = cache.deleteSync(hdlKey(key))
  }

  case object WatermarkSettings extends HdlCache[String, WatermarkSetting] {
    private val cache =
      new Cache[cacheValue[WatermarkSetting]](CacheConfig(ttl = Some(HdlTtl.watermarkSettingsRedisTTL)))

    override def get(key: String): Option[WatermarkSetting] =
      getValue(cache.getSync(hdlKey(key), "WatermarkSettings", refreshTTL = false))

    override def set(key: String, value: WatermarkSetting): Unit =
      cache.setSync(hdlKey(key), "WatermarkSettings", cacheValue(value, HdlTtl.watermarkSettingsRedisTTL))

    override def evict(key: String): Unit = cache.deleteSync(hdlKey(key))
  }
}
