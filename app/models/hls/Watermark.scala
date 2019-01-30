package models.hls

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.komrade.thrift.{Partner, User}
import javax.inject.Inject
import models.auth.JWTWrapper
import models.common.RtmQueryParams
import services.komrade.KomradeClient
import utils.DateTime

import scala.concurrent.{ExecutionContext, Future}

case class WatermarkDetails(partner: Partner, user: User)

trait Watermark {
  def augmentQuery(query: RtmQueryParams, jwt: JWTWrapper): Future[RtmQueryParams]
}

class WatermarkImpl @Inject()(komrade: KomradeClient)
                             (implicit ex: ExecutionContext)
  extends Watermark with LazyLogging {
  final val labelParam = "label"

  override def augmentQuery(query: RtmQueryParams, jwt: JWTWrapper): Future[RtmQueryParams] = {
    getWatermarkDetails(jwt.audienceId, jwt.subjectId) flatMap { u =>
      generateWatermark(u) match {
        case Some(watermark) =>
          if (query.params.contains(labelParam)) {
              logger.warn("labelWillBeOverwritten")("queryParams" -> query.params)
          }
          Future.successful(RtmQueryParams(
            query.media,
            query.path,
            query.params - labelParam + (labelParam -> watermark)
          ))
        case None =>
          logger.error("failedToGenerateWatermark")(
            "rtmQueryParams" -> query.params,
            "partnerId" -> jwt.audienceId,
            "userId" -> jwt.subjectId
          )
          Future.failed(new Exception("Failed to generate watermark"))
      }
    }
  }

  private def getWatermarkDetails(partnerId: String, userId: String): Future[WatermarkDetails] = {
    for {
      partner <- komrade.getPartner(partnerId)
      user <- komrade.getUser(partnerId, userId)
    } yield WatermarkDetails(partner, user)
  }

  /**
    * generateWatermark implements similar logic as ECOMSAAS's Label method:
    * https://git.taservs.net/ecom/ecomsaas/blob/367389cbfb68b5cfa157fd8913d270b288baa87a/wc/com.evidence.api/com.evidence.api/evidence/Stream.cs#L671
    */
  private def generateWatermark(watermarkDetails: WatermarkDetails): Option[String] = {
    watermarkDetails.user.username match {
      case Some(username) =>
        watermarkDetails.partner.domain match {
          case Some(domain) =>
            Some(s"Viewed by $username ($domain) on ${DateTime.getUtcDate}")
          case None =>
            logger.error("failedToRetrievePartnerDomain")()
            None
        }
      case None =>
        logger.error("failedToRetrieveUsername")()
        None
    }
  }

}
