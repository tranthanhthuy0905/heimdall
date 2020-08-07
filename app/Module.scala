import com.google.inject.{AbstractModule, Singleton}
import models.auth.{Authorizer, AuthorizerImpl, StreamingSessionData, StreamingSessionDataImpl}
import org.apache.curator.framework.CuratorFramework
import services.apidae.{ApidaeClient, ApidaeClientImpl}
import services.audit.{AuditClient, AuditClientImpl}
import services.document.{DocumentClient, DocumentClientImpl}
import services.dredd.{DreddClient, DreddClientImpl}
import services.global.HeimdallApplicationLifecycle
import services.komrade.{KomradeClient, KomradeClientImpl}
import services.nino.{NinoClient, NinoClientImpl}
import services.pdp.{PdpClient, PdpClientImpl, PdpEnable, PdpEnableImpl}
import services.rti.{RtiClient, RtiClientImpl}
import services.rtm.{RtmClient, RtmClientImpl}
import services.sessions.{SessionsClient, SessionsClientImpl}
import services.zookeeper.{HeimdallLoadBalancer, HeimdallLoadBalancerProvider, ZookeeperClientProvider}

class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[HeimdallApplicationLifecycle]).asEagerSingleton()
    // Bindings
    bind(classOf[AuditClient]).to(classOf[AuditClientImpl])
    bind(classOf[Authorizer]).to(classOf[AuthorizerImpl])
    bind(classOf[DreddClient]).to(classOf[DreddClientImpl])
    bind(classOf[KomradeClient]).to(classOf[KomradeClientImpl])
    bind(classOf[NinoClient]).to(classOf[NinoClientImpl])
    bind(classOf[PdpClient]).to(classOf[PdpClientImpl])
    bind(classOf[PdpEnable]).to(classOf[PdpEnableImpl])
    bind(classOf[RtmClient]).to(classOf[RtmClientImpl])
    bind(classOf[RtiClient]).to(classOf[RtiClientImpl])
    bind(classOf[ApidaeClient]).to(classOf[ApidaeClientImpl])
    bind(classOf[DocumentClient]).to(classOf[DocumentClientImpl])
    bind(classOf[SessionsClient]).to(classOf[SessionsClientImpl])
    bind(classOf[StreamingSessionData]).to(classOf[StreamingSessionDataImpl])
    // Providers
    bind(classOf[CuratorFramework]).toProvider(classOf[ZookeeperClientProvider]).in(classOf[Singleton])
    bind(classOf[HeimdallLoadBalancer]).toProvider(classOf[HeimdallLoadBalancerProvider]).in(classOf[Singleton])
  }
}
