import java.time.Clock

import com.google.inject.{AbstractModule, Singleton}
import models.auth.{Authorizer, AuthorizerImpl}
import org.apache.curator.framework.CuratorFramework
import services.dredd.{DreddClient, DreddClientImpl}
import services.sessions.{SessionsClient, SessionsClientImpl}
import services.zookeeper.{ZookeeperClientProvider, ZookeeperServerSet, ZookeeperServerSetProvider}

class Module extends AbstractModule {

  override def configure() = {
    // Bindings
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    bind(classOf[DreddClient]).to(classOf[DreddClientImpl])
    bind(classOf[Authorizer]).to(classOf[AuthorizerImpl])
    bind(classOf[SessionsClient]).to(classOf[SessionsClientImpl])
    // Providers
    bind(classOf[CuratorFramework]).toProvider(classOf[ZookeeperClientProvider]).in(classOf[Singleton])
    bind(classOf[ZookeeperServerSet]).toProvider(classOf[ZookeeperServerSetProvider]).in(classOf[Singleton])
  }
}



