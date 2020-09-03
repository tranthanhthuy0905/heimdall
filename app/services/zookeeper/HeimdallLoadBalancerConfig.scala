package services.zookeeper

import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config

import scala.util.{Failure, Success, Try}

case class HeimdallLoadBalancerConfig(enableCache: Boolean, reloadIntervalMs: Int, enableRTMv2: Boolean)

object HeimdallLoadBalancerConfig extends LazyLogging {
  final val enableCacheDefault      = true
  final val reloadIntervalMsDefault = 100
  final val enableRTMv2Default      = false

  def apply(config: Config): HeimdallLoadBalancerConfig = {
    val enableCache         = getConfigValue[Boolean](config, "enable_cache", enableCacheDefault, config.getBoolean)
    val reloadIntervalMs    = getConfigValue[Int](config, "reload_interval_ms", reloadIntervalMsDefault, config.getInt)
    val enableRTMv2         = getConfigValue[Boolean](config, "enable_rtmv2", enableRTMv2Default, config.getBoolean)
    HeimdallLoadBalancerConfig(enableCache, reloadIntervalMs, enableRTMv2)
  }

  private[this] def getConfigValue[T](config: Config, name: String, default: T, fn: String => T): T = {
    Try(fn(s"heimdall.load_balancer.$name")) match {
      case Success(value) =>
        value
      case Failure(exception) =>
        logger.warn("failedToFindConfigValue")(
          "exception" -> exception.getMessage,
          "name"      -> name,
          "default"   -> default,
          "message"   -> s"heimdall.load_balancer.$name defaults to $default"
        )
        default
    }
  }
}
