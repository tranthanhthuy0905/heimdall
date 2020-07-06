package services.pdp

import com.google.inject.Inject
import com.typesafe.config.Config
import javax.inject.Singleton

trait PdpEnable{
  def isSet: Boolean
}

/** why i have this simple class.
  *  - this configuration is used by PermValidationAction to switch between Nino and PDP.
  *  - it is too heavy to add Config to PermValidationAction constructor.
  *  - this class will be removed when PDP is stable
  */
@Singleton
class PdpEnableImpl @Inject() (config: Config) extends PdpEnable {
  val isSet: Boolean = config.getBoolean("edc.service.pdp.enabled")
}
