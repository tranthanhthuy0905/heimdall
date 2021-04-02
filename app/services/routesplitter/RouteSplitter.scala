package services.routesplitter

import java.util.UUID

import com.google.inject.{Inject, Provider}
import com.typesafe.config.Config

trait RouteSplitter {
  def getApiVersion(key: UUID): Int
}

case class DefaultRouteSplitter(percentageThreshold: Int) extends RouteSplitter {
  override def getApiVersion(key: UUID): Int = {
      val mod = java.lang.Math.floorMod(key.toString.hashCode, 100)
      if (mod < percentageThreshold) 1 else 2
  }
}

case class NoOpRouteSplitter(config: Config) extends RouteSplitter {
  override def getApiVersion(key: UUID): Int = {
    if (config.hasPath("service.route_splitter.preferred_version"))
      config.getInt("service.route_splitter.preferred_version")
    else
      1
  }
}

class RouteSplitterProvider @Inject()(config: Config)
  extends Provider[RouteSplitter] {
  def get(): RouteSplitter = {
    if (config.getBoolean("service.route_splitter.enabled"))
      DefaultRouteSplitter(config.getInt("service.route_splitter.percentage_threshold"))
    else
      NoOpRouteSplitter(config)
  }
}
