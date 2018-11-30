package modules

import com.google.inject.Singleton
import models.auth.{Authorizer, AuthorizerImpl}
import org.apache.curator.framework.CuratorFramework
import org.scalatest.mockito.MockitoSugar
import services.dredd.{DreddClient, DreddClientImpl}
import services.rtm.{RtmClient, RtmClientImpl}
import services.sessions.{SessionsClient, SessionsClientImpl}
import services.zookeeper.{ZookeeperClientProvider, ZookeeperServerSet, ZookeeperServerSetProvider}

class MockModule extends Module with MockitoSugar {

  override def configure() = {
    bind(classOf[DreddClient]).toInstance(mock[DreddClient])
    bind(classOf[Authorizer]).toInstance(mock[Authorizer])
    bind(classOf[SessionsClient]).toInstance(mock[SessionsClient])
    bind(classOf[RtmClient]).toInstance(mock[RtmClient])

    bind(classOf[CuratorFramework]).toInstance(mock[CuratorFramework])
    bind(classOf[ZookeeperServerSet]).toInstance(mock[ZookeeperServerSet])
  }

}
