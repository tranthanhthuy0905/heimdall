package services.komrade

import com.evidence.service.common.finagle.FinagleClient
import com.evidence.service.common.finagle.FutureConverters._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.komrade.thrift._
import com.evidence.service.komrade.thrift.KomradeService
import com.evidence.service.thrift.v2.Authorization
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.cache.AsyncCacheApi

import scala.concurrent.duration.{Duration, MINUTES}
import scala.concurrent.{ExecutionContext, Future}

trait KomradeClient {
  def getUser(partnerId: String, userId: String): Future[Option[String]]
  def getPartner(partnerId: String): Future[Option[String]]
}

@Singleton
class CachedKomradeClientImpl @Inject()(config: Config,
                                        cache: AsyncCacheApi)(implicit ex: ExecutionContext)
    extends KomradeClient
    with LazyLogging {

  private val client: KomradeService.MethodPerEndpoint = {
    val env    = FinagleClient.getEnvironment(config)
    val dest   = FinagleClient.newThriftUrl("com.evidence.service.komrade", env, "thrift_ssl")
    val client = FinagleClient.newThriftClient().build[KomradeService.MethodPerEndpoint](dest)
    client
  }

  private val auth: Authorization = {
    val secret   = config.getString("edc.service.komrade.thrift_auth_secret")
    val authType = config.getString("edc.service.komrade.thrift_auth_type")
    Authorization(Option(authType), Option(secret))
  }

  // Streaming use komrade to get username and agency name for watermark string only
  // it is safe to cached results for those calls since Komrade call is expensive (query DB)
  private val cacheUserExpired = Duration(5, MINUTES)
  private val cachePartnerExpired = Duration(30, MINUTES)

  override def getUser(partnerId: String, userId: String): Future[Option[String]] = {
    cache.getOrElseUpdate[Option[String]]((partnerId, userId).toString, cacheUserExpired) {
      logger.debug("getUser")("partnerId" -> partnerId, "userId" -> userId)
      client.getUser(partnerId, userId).toScalaFuture.map(_.username)
    }
  }

  override def getPartner(partnerId: String): Future[Option[String]] = {
    cache.getOrElseUpdate[Option[String]](partnerId, cachePartnerExpired) {
      logger.debug("getPartner")("partnerId" -> partnerId)
      client.getPartner(partnerId).toScalaFuture.map(_.domain)
    }
  }

}
