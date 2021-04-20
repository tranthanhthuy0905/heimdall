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
import utils.{HdlCache, HdlTtl}

import scala.concurrent.duration.{Duration, MINUTES}
import scala.concurrent.{ExecutionContext, Future}

trait KomradeClient {
  def getUser(partnerId: String, userId: String): Future[Option[String]]
  def getPartner(partnerId: String): Future[Option[String]]
  def getWatermarkSettings(partnerId: String): Future[Option[WatermarkSetting]]
  def updateWatermarkSettings(partnerId: String, watermarkSetting: WatermarkSetting): Future[Unit]
}

@Singleton
class CachedKomradeClientImpl @Inject()(config: Config, cache: AsyncCacheApi)(implicit ex: ExecutionContext)
    extends KomradeClient
    with LazyLogging {

  private val client: KomradeService.MethodPerEndpoint = {
    val env    = FinagleClient.getEnvironment(config)
    val dest   = FinagleClient.newThriftUrl("com.evidence.service.komrade", env, "thrift_ssl")
    val client = FinagleClient.newThriftClient().build[KomradeService.MethodPerEndpoint](dest)
    client
  }

  // test authorization when init class
  private val auth: Authorization = {
    val secret   = config.getString("edc.service.komrade.thrift_auth_secret")
    val authType = config.getString("edc.service.komrade.thrift_auth_type")
    Authorization(Option(authType), Option(secret))
  }

  // Streaming use komrade to get username and agency name for watermark string only
  // it is safe to cached results for those calls since Komrade call is expensive (query DB)
  override def getUser(partnerId: String, userId: String): Future[Option[String]] = {
    val key = s"hdl-$partnerId-$userId"
    cache.getOrElseUpdate[Option[String]](key, HdlTtl.usernameMemTTL) {
      HdlCache.Username
        .get(key)
        .map { un =>
          Future.successful(Some(un))
        }
        .getOrElse {
          logger.debug("getUser")("partnerId" -> partnerId, "userId" -> userId)
          val res = client.getUser(partnerId, userId).toScalaFuture.map { u =>
            {
              u.username.foreach(un => HdlCache.Username.set(key, un))
              u.username
            }
          }
          res
        }
    }
  }

  override def getPartner(partnerId: String): Future[Option[String]] = {
    val key = s"hdl-$partnerId"
    cache.getOrElseUpdate[Option[String]](key, HdlTtl.domainMemTTL) {
      HdlCache.AgencyDomain
        .get(key)
        .map { un =>
          Future.successful(Some(un))
        }
        .getOrElse {
          logger.debug("getPartner")("partnerId" -> partnerId)
          val res = client.getPartner(partnerId).toScalaFuture.map { p =>
            {
              p.domain.foreach(d => HdlCache.AgencyDomain.set(key, d))
              p.domain
            }
          }
          res
        }
    }
  }

  override def getWatermarkSettings(partnerId: String): Future[Option[WatermarkSetting]] = {
    val key = s"hdl-$partnerId"
    HdlCache.WatermarkSettings
      .get(key)
      .map { un =>
        Future.successful(Some(un))
      }
      .getOrElse {
        logger.debug("getWatermarkSettings")("partnerId" -> partnerId)
        val request = GetWatermarkSettingRequest(partnerId)
        val res = client.getWatermarkSetting(auth, request).toScalaFuture.map { s =>
          s.setting.foreach(x => HdlCache.WatermarkSettings.set(key, x))
          s.setting
        }
        res
      }
  }

  override def updateWatermarkSettings(partnerId: String, watermarkSetting: WatermarkSetting): Future[Unit] = {
    val key = s"hdl-$partnerId"
    client.createOrUpdateWatermarkSetting(auth, watermarkSetting).toScalaFuture
      .map( _ => HdlCache.WatermarkSettings.set(key, watermarkSetting))
  }
}
