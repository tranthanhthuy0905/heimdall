package utils

import com.evidence.service.common.ServiceGlobal

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
  // jitter is time tolerance between local time and redis server time due to
  // - network
  // - operation overhead
  // - internal implementation of redis TTL: might not sub-seconds precise (e.g. drop values in batch)
  // use to filter-out false positive cache expired stats
  private val jitter = Duration(3, TimeUnit.SECONDS).toNanos

  def apply[K](value: K, ttl: Duration): cacheValue[K] = cacheValue(
    value = value,
    expired = Instant.now().plusNanos(ttl.toNanos + jitter),
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
  private def getValue[K](someValue: Option[cacheValue[K]], cacheKey: String): Option[K] = {
    someValue.flatMap(
      value =>
        if (Instant.now().isBefore(value.expired)) {
          Some(value.value)
        } else {
          ServiceGlobal.statsd.increment("cache.expired", s"cache_key:$cacheKey")
          None
      }
    )
  }

  case object Username extends HdlCache[String, String] {
    private val cacheKey = "Username"
    private val cache    = new Cache[cacheValue[String]](CacheConfig(ttl = Some(HdlTtl.usernameRedisTTL)))

    override def get(key: String): Option[String] =
      getValue(cache.getSync(hdlKey(key), cacheKey, refreshTTL = false), cacheKey)

    override def set(key: String, value: String): Unit =
      cache.setSync(hdlKey(key), cacheKey, cacheValue(value, HdlTtl.usernameRedisTTL))

    override def evict(key: String): Unit = cache.deleteSync(hdlKey(key))
  }

  case object AgencyDomain extends HdlCache[String, String] {
    private val cacheKey = "AgencyDomain"
    private val cache    = new Cache[cacheValue[String]](CacheConfig(ttl = Some(HdlTtl.domainRedisTTL)))

    override def get(key: String): Option[String] =
      getValue(cache.getSync(hdlKey(key), cacheKey, refreshTTL = false), cacheKey)

    override def set(key: String, value: String): Unit =
      cache.setSync(hdlKey(key), cacheKey, cacheValue(value, HdlTtl.domainRedisTTL))

    override def evict(key: String): Unit = cache.deleteSync(hdlKey(key))
  }

  case object PresignedUrl extends HdlCache[String, URL] {
    private val cacheKey = "PresignedUrl"
    private val cache    = new Cache[cacheValue[String]](CacheConfig(ttl = Some(HdlTtl.urlRedisTTL)))

    override def get(key: String): Option[URL] =
      getValue(cache.getSync(hdlKey(key), cacheKey, refreshTTL = false), cacheKey).map(url => new URL(url))

    override def set(key: String, value: URL): Unit =
      cache.setSync(hdlKey(key), cacheKey, cacheValue(value.toString, HdlTtl.urlRedisTTL))

    override def evict(key: String): Unit = cache.deleteSync(hdlKey(key))
  }

  case object WatermarkSettings extends HdlCache[String, WatermarkSetting] {
    private val cacheKey = "WatermarkSettings"
    private val cache =
      new Cache[cacheValue[WatermarkSetting]](CacheConfig(ttl = Some(HdlTtl.watermarkSettingsRedisTTL)))

    override def get(key: String): Option[WatermarkSetting] =
      getValue(cache.getSync(hdlKey(key), cacheKey, refreshTTL = false), cacheKey)

    override def set(key: String, value: WatermarkSetting): Unit =
      cache.setSync(hdlKey(key), cacheKey, cacheValue(value, HdlTtl.watermarkSettingsRedisTTL))

    override def evict(key: String): Unit = cache.deleteSync(hdlKey(key))
  }
}
