import models.auth.{Authorizer, StreamingSessionData}
import org.apache.curator.framework.CuratorFramework
import org.scalatest.mockito.MockitoSugar
import services.audit.AuditClient
import services.dredd.DreddClient
import services.komrade.KomradeClient
import services.nino.NinoClient
import services.rti.RtiClient
import services.rtm.RtmClient
import services.sessions.SessionsClient
import services.zookeeper.ZookeeperServerSet

class MockModule extends Module with MockitoSugar {
  override def configure() = {
    bind(classOf[AuditClient]).toInstance(mock[AuditClient])
    bind(classOf[Authorizer]).toInstance(mock[Authorizer])
    bind(classOf[DreddClient]).toInstance(mock[DreddClient])
    bind(classOf[KomradeClient]).toInstance(mock[KomradeClient])
    bind(classOf[NinoClient]).toInstance(mock[NinoClient])
    bind(classOf[RtiClient]).toInstance(mock[RtiClient])
    bind(classOf[RtmClient]).toInstance(mock[RtmClient])
    bind(classOf[SessionsClient]).toInstance(mock[SessionsClient])
    bind(classOf[StreamingSessionData]).toInstance(mock[StreamingSessionData])

    bind(classOf[CuratorFramework]).toInstance(mock[CuratorFramework])
    bind(classOf[ZookeeperServerSet]).toInstance(mock[ZookeeperServerSet])
  }
}
