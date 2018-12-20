package modules

import models.auth.Authorizer
import org.apache.curator.framework.CuratorFramework
import org.scalatest.mockito.MockitoSugar
import services.audit.AuditClient
import services.dredd.DreddClient
import services.rtm.RtmClient
import services.sessions.SessionsClient
import services.zookeeper.ZookeeperServerSet

class MockModule extends Module with MockitoSugar {

  override def configure() = {
    bind(classOf[AuditClient]).toInstance(mock[AuditClient])
    bind(classOf[DreddClient]).toInstance(mock[DreddClient])
    bind(classOf[Authorizer]).toInstance(mock[Authorizer])
    bind(classOf[SessionsClient]).toInstance(mock[SessionsClient])
    bind(classOf[RtmClient]).toInstance(mock[RtmClient])

    bind(classOf[CuratorFramework]).toInstance(mock[CuratorFramework])
    bind(classOf[ZookeeperServerSet]).toInstance(mock[ZookeeperServerSet])
  }

}