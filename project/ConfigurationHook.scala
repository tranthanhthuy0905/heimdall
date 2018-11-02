import play.sbt.PlayRunHook

object ConfigurationHook {
  def apply(): PlayRunHook = {

    object ConfigurationSetting extends PlayRunHook {
      private def safeSetProperty(name: String, value: String) = {
        if(Option(System.getProperty(name)).getOrElse("").isEmpty) {
          System.setProperty(name, value)
        }
      }

      override def beforeStarted(): Unit = {
        safeSetProperty("config.resource", "env/localdev.conf")
        safeSetProperty("config.trace", "loads")
      }
    }

    ConfigurationSetting
  }
}
