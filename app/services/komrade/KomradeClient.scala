package services.komrade

import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.finagle.FutureConverters._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.komrade.thrift._
import com.evidence.service.komrade.thrift.KomradeService
import com.evidence.service.thrift.v2.Authorization
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

trait KomradeClient {
  def getUser(partnerId: String, userId: String): Future[User]
  def getPartner(partnerId: String): Future[Partner]
}

@Singleton
class KomradeClientImpl @Inject() (config: Config) (implicit ex: ExecutionContext) extends KomradeClient with LazyLogging {

  private val client: KomradeService.MethodPerEndpoint = {
    val env = FinagleClient.getEnvironment(config)
    val dest = FinagleClient.newThriftUrl("com.evidence.service.komrade", env, "thrift_ssl")
    val client = FinagleClient.newThriftClient().build[KomradeService.MethodPerEndpoint](dest)
    client
  }

  private val auth: Authorization = {
    val secret = config.getString("edc.service.komrade.thrift_auth_secret")
    val authType = config.getString("edc.service.komrade.thrift_auth_type")
    Authorization(Option(authType), Option(secret))
  }

  override def getUser(partnerId: String, userId: String): Future[User] = {
    logger.debug("getUser")("partnerId" -> partnerId, "userId" -> userId)
    client.getUser(partnerId, userId).toScalaFuture
  }

  override def getPartner(partnerId: String): Future[Partner] = {
    logger.debug("getPartner")("partnerId" -> partnerId)
    client.getPartner(partnerId).toScalaFuture
  }

}
