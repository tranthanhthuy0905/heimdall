package services.pdp

import java.util.UUID

import com.axon.pdp.protos.v1.pdp_service._
import com.evidence.api.thrift.v1.EntityDescriptor
import com.evidence.service.common.auth.jwt.{JWTWrapper, VerifyingJWTParser}
import com.evidence.service.common.auth.{CachingJOSEComponentFactory, KeyManager}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.{Convert, Tid}
import com.evidence.service.thrift.v2.ServiceErrorCode.Badrequest
import com.evidence.service.thrift.v2.ServiceException
import com.nimbusds.jwt.JWT
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import com.axon.pdp.protos.v1.pdp_service.{PdpServiceGrpc, Tid => PTid}
import com.evidence.service.common.monitoring.statsd.StrictStatsD

import scala.concurrent.{ExecutionContext, Future}

trait PdpClient {
  def enforceBatch(jwt: String, entities: List[EntityDescriptor], action: String): Future[Boolean]
}

@Singleton
class PdpClientImpl @Inject()(config: Config)(implicit ex: ExecutionContext)
    extends PdpClient
    with PdpClientHelper
    with LazyLogging
    with StrictStatsD {
  protected val keyManager: KeyManager                        = KeyManager.apply(config)
  protected val componentFactory: CachingJOSEComponentFactory = new CachingJOSEComponentFactory(keyManager)
  protected val parser                                        = new VerifyingJWTParser(componentFactory)

  private val client: PdpServiceGrpc.PdpServiceStub = buildPdpClient(
    config.getString("edc.service.pdp.host"),
    config.getInt("edc.service.pdp.port"),
    config.getString("edc.service.pdp.secret")
  )

  override def enforceBatch(jwt: String, entities: List[EntityDescriptor], action: String): Future[Boolean] = {
    val user: Tid = jwtToUser(jwt)
    val ents: Seq[PTid] = entities.map(ed => PTid(ed.entityType.name, ed.id, ed.domain.getOrElse("")))

    val batchRequest: EnforceBatchRequest =
      EnforceBatchRequest(
        subject = toProtoTid(user),
        resources = ents,
        action = action,
        requestInfo = Some(
          RequestInfo(
            correlationId = UUID.randomUUID().toString,
            callingService = "heimdall"
          )
        )
      )
    executionTime(
      aspect = "PdpClient.enforceBatch",
      future =
        client
          .enforceBatch(batchRequest)
          .map(r => r.result.forall(_.granted == true))
    )
  }

  private def jwtToUser(jwtStr: String): Tid =
    parser.parse(jwtStr) match {
      case Left(err) =>
        logger.info("auth_error")("error" -> err)
        throw ServiceException(Badrequest, Some(s"Invalid JWT: $err"))
      case Right(jwt: JWT) =>
        val wrapper = JWTWrapper(jwt)
        wrapper.subjectDomain match {
          case None         => throw ServiceException(Badrequest, Some("User domain is required"))
          case Some(domain) => Tid(wrapper.subjectType, wrapper.subjectId, Convert.fromGuidToTidFormat(domain))
        }
    }
}
