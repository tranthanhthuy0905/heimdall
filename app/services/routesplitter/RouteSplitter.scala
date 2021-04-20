package services.routesplitter

import java.util.UUID

import com.evidence.service.common.monitoring.statsd.StrictStatsD
import com.google.inject.{Inject, Provider}
import com.typesafe.config.Config

trait RouteSplitter extends StrictStatsD {
  def getApiVersion(key: UUID, defaultVersion: Int): Int

  def withStatsLogged(version: Int): Int = {
    statsd.increment(s"route_splitter", s"rtm:v${version}")
    version
  }
}

case class DefaultRouteSplitter(percentageThreshold: Int) extends RouteSplitter with StrictStatsD {
  private def doGetApiVersion(key: UUID): Int = {
    val mod = java.lang.Math.floorMod(key.toString.hashCode, 100)
    if (mod < percentageThreshold) 1 else 2
  }
  override def getApiVersion(key: UUID, defaultVersion: Int): Int = withStatsLogged(doGetApiVersion(key))

}

case class NoOpRouteSplitter() extends RouteSplitter with StrictStatsD {
  override def getApiVersion(key: UUID, defaultVersion: Int): Int = withStatsLogged(defaultVersion)
}

class RouteSplitterProvider @Inject()(config: Config) extends Provider[RouteSplitter] {

  def get(): RouteSplitter = {
    if (config.getBoolean("service.route_splitter.enabled"))
      DefaultRouteSplitter(config.getInt("service.route_splitter.percentage_threshold"))
    else
      NoOpRouteSplitter()
  }
}