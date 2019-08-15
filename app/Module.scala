import com.google.inject.{AbstractModule, Singleton}
import models.auth.{Authorizer, AuthorizerImpl, StreamingSessionData, StreamingSessionDataImpl}
import org.apache.curator.framework.CuratorFramework
import services.audit.{AuditClient, AuditClientImpl}
import services.dredd.{DreddClient, DreddClientImpl}
import services.global.HeimdallApplicationLifecycle
import services.komrade.{KomradeClient, KomradeClientImpl}
import services.nino.{NinoClient, NinoClientImpl}
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
    bind(classOf[RtmClient]).to(classOf[RtmClientImpl])
    bind(classOf[RtiClient]).to(classOf[RtiClientImpl])
    bind(classOf[SessionsClient]).to(classOf[SessionsClientImpl])
    bind(classOf[StreamingSessionData]).to(classOf[StreamingSessionDataImpl])
    // Providers
    bind(classOf[CuratorFramework]).toProvider(classOf[ZookeeperClientProvider]).in(classOf[Singleton])
    bind(classOf[HeimdallLoadBalancer]).toProvider(classOf[HeimdallLoadBalancerProvider]).in(classOf[Singleton])
  }
}
