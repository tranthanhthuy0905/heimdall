import com.google.inject.AbstractModule
import java.time.Clock

import services.dredd.{DreddClient, DreddClientImpl}
import services.sessions.{SessionsClient, SessionsClientImpl}

class Module extends AbstractModule {

  override def configure() = {
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    bind(classOf[DreddClient]).to(classOf[DreddClientImpl])
    bind(classOf[SessionsClient]).to(classOf[SessionsClientImpl])
  }
}
