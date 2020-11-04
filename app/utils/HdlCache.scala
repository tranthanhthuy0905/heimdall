package utils

import java.net.URL
import java.util.concurrent.TimeUnit

import com.evidence.service.common.cache.{Cache, CacheConfig}
import com.evidence.service.common.config.Configuration

import scala.concurrent.duration.{Duration, FiniteDuration}

trait HdlCache[K, T] {
  def get(key: K): Option[T]
  def set(key: K, value: T): Unit
  def evict(key: K): Unit
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
}

case object HdlCache {
  case object Username extends HdlCache[String, String] {
    private val cache = new Cache[String](CacheConfig(ttl = Some(HdlTtl.usernameRedisTTL)))

    override def get(key: String): Option[String] =
      cache.getSync(key, "HdlUsername", refreshTTL = false)

    override def set(key: String, value: String): Unit =
      cache.setSync(key, "HdlUsername", value)

    override def evict(key: String): Unit = cache.deleteSync(key)
  }

  case object AgencyDomain extends HdlCache[String, String] {
    private val cache = new Cache[String](CacheConfig(ttl = Some(HdlTtl.domainRedisTTL)))

    override def get(key: String): Option[String] =
      cache.getSync(key, "HdlAgencyDomain", refreshTTL = false)

    override def set(key: String, value: String): Unit =
      cache.setSync(key, "HdlAgencyDomain", value)

    override def evict(key: String): Unit = cache.deleteSync(key)
  }

  case object PresignedUrl extends HdlCache[String, URL] {
    private val cache = new Cache[String](CacheConfig(ttl = Some(HdlTtl.urlRedisTTL)))

    override def get(key: String): Option[URL] =
      cache.getSync(key, "HdlPresignedUrl", refreshTTL = false).map(url => new URL(url))

    override def set(key: String, value: URL): Unit =
      cache.setSync(key, "HdlPresignedUrl", value.toString)

    override def evict(key: String): Unit = cache.deleteSync(key)
  }
}
