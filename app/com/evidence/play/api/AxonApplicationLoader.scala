package com.evidence.play.api

import play.api.{ApplicationLoader,Configuration}
import play.api.inject.guice._

class AxonApplicationLoader extends GuiceApplicationLoader() {
  override def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {

    val configFile = configFileFromEnv(context)
    val configByEnv = Configuration.load(context.environment, Map("config.file" -> configFile))

    initialBuilder
      .in(context.environment)
      .loadConfig(configByEnv ++ context.initialConfiguration)
      .overrides(overrides(context): _*)
  }

  /**
    * Get environment from initial configuration, the environment variable must to set before bootstrap the application
    * in local env, it will be set by sbt, others we have to manually set it in order to not get any accident.
    * @param context current app context
    * @return path to env configuration file
    */
  def configFileFromEnv(context: ApplicationLoader.Context): String =
    context.initialConfiguration.get[String]("service.env") match {
      case "dev" => "conf/envs/dev.conf"
      case _ => throw new ExceptionInInitializerError(s"The environment variable is not set or empty!!!")
    }
}
