import models.auth.{Authorizer, StreamingSessionData}
import org.apache.curator.framework.CuratorFramework
import org.scalatest.mockito.MockitoSugar
import services.apidae.{ApidaeClient, ApidaeClientImpl}
import services.audit.AuditClient
import services.document.{DocumentClient, DocumentClientImpl}
import services.dredd.DreddClient
import services.komrade.KomradeClient
import services.pdp.{PdpClient, PdpClientImpl}
import services.queue.ProbeNotifier
import services.rti.RtiClient
import services.rtm.RtmClient
import services.sessions.SessionsClient
import services.zookeeper.HeimdallLoadBalancer

class MockModule extends Module with MockitoSugar {
  override def configure() = {
    bind(classOf[AuditClient]).toInstance(mock[AuditClient])
    bind(classOf[Authorizer]).toInstance(mock[Authorizer])
    bind(classOf[DreddClient]).toInstance(mock[DreddClient])
    bind(classOf[KomradeClient]).toInstance(mock[KomradeClient])
    bind(classOf[PdpClient]).toInstance(mock[PdpClientImpl])
    bind(classOf[RtiClient]).toInstance(mock[RtiClient])
    bind(classOf[ApidaeClient]).to(classOf[ApidaeClientImpl])
    bind(classOf[RtmClient]).toInstance(mock[RtmClient])
    bind(classOf[SessionsClient]).toInstance(mock[SessionsClient])
    bind(classOf[StreamingSessionData]).toInstance(mock[StreamingSessionData])
    bind(classOf[DocumentClient]).toInstance(mock[DocumentClientImpl])

    bind(classOf[CuratorFramework]).toInstance(mock[CuratorFramework])
    bind(classOf[HeimdallLoadBalancer]).toInstance(mock[HeimdallLoadBalancer])
    bind(classOf[ProbeNotifier]).toInstance(mock[ProbeNotifier])
  }
}
