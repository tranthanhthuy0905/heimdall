package services.global

import com.evidence.service.common.ServiceGlobal
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monitoring.nagios.NagiosConfig
import com.evidence.service.common.monitoring.statsd.StatsdConfig
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class HeimdallApplicationLifecycle @Inject()(lifecycle: ApplicationLifecycle, config: Config) extends LazyLogging {

  ServiceGlobal.initialize(
    new com.evidence.service.common.config.ServiceConfig(config),
    new NagiosConfig(config),
    new StatsdConfig(config)
  )

  logger.info("heimdallApplicationLifecycle")("message" -> "Nagios and StatsD Checks Initialized")
  lifecycle.addStopHook { () =>
    Future.successful(ServiceGlobal.shutdown())
  }
}
