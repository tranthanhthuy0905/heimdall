package actions

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.komrade.thrift.{Partner, User}
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import services.komrade.KomradeClient
import utils.DateTime

import scala.concurrent.{ExecutionContext, Future}

case class WatermarkAction @Inject()(komrade: KomradeClient)(
  implicit val executionContext: ExecutionContext
) extends ActionRefiner[HeimdallRequest, HeimdallRequest] {
  def refine[A](
    input: HeimdallRequest[A]
  ): Future[Either[Results.Status, HeimdallRequest[A]]] =
    input.path match {
      case path if path.contains("thumbnail") =>
        /** isMulticam determines if heimdall is dealing with multicam request or not.
          * Normally thumbnails multicam requests contain `customlayout`. However, in some cases when customlayout is not
          * provided, RTM will ignore width and height values and generate thumbnails using `defaultLayout`.
          * See https://git.taservs.net/ecom/rtm/blob/6a7a6d1d2b2413c244262b49c5dfd151c4b67145/src/rtm/server/core/combine.go#L130
          * Because of the variety of use-cases, heimdall simply generates label for all multicam thumbnail requests.
          */
        if (isMulticam(input) || isResolutionHighEnough(input)) {
          getWatermarkedRequest(input)
        } else {
          Future.successful(Right(input))
        }
      case _ =>
        getWatermarkedRequest(input)
    }

  private def getWatermarkedRequest[A](
    input: HeimdallRequest[A]
  ): Future[Either[Results.Status, HeimdallRequest[A]]] = {
    val watermark = for {
      partner <- komrade.getPartner(input.audienceId)
      user <- komrade.getUser(input.audienceId, input.subjectId)
    } yield Watermark(partner, user)
    watermark.map {
      _ match {
        case Some(value) =>
          Right(HeimdallRequest(input, value))
        case None =>
          Left(Results.InternalServerError)
      }
    }
  }

  private def isMulticam[A](request: HeimdallRequest[A]): Boolean = {
    request.media.evidenceIds.length > 1
  }

  private def isResolutionHighEnough[A](
    request: HeimdallRequest[A]
  ): Boolean = {
    val widthIsAboveThreshold =
      isAboveThreshold(request, paramName = "width", threshold = 400)
    val heightIsAboveThreshold =
      isAboveThreshold(request, paramName = "height", threshold = 200)
    widthIsAboveThreshold && heightIsAboveThreshold
  }

  /**
    * isAboveThreshold checks if a certain parameter value is equal to or above a threshold.
    *
    *  @param request heimdall incoming request.
    *  @param paramName name of a parameter that will be validated.
    *  @param threshold empirically chosen threshold value.
    *  @return Boolean value indicating if validated parameter presents in the query and equal to or
    *          greater than the threshold.
    */
  private def isAboveThreshold[A](request: HeimdallRequest[A],
                                  paramName: String,
                                  threshold: Int): Boolean = {
    request.queryString
      .getOrElse(paramName, Seq())
      .headOption
      .getOrElse("0")
      .toInt >= threshold
  }

}

object Watermark extends LazyLogging {

  /**
    * generateWatermark implements similar logic as ECOMSAAS's Label method:
    * https://git.taservs.net/ecom/ecomsaas/blob/367389cbfb68b5cfa157fd8913d270b288baa87a/wc/com.evidence.api/com.evidence.api/evidence/Stream.cs#L671
    */
  def apply(partner: Partner, user: User): Option[String] = {
    user.username match {
      case Some(username) =>
        partner.domain match {
          case Some(domain) =>
            Some(s"Viewed by $username ($domain) on ${DateTime.getUtcDate}")
          case None =>
            logger.error("failedToRetrievePartnerDomain")(
              "user" -> user,
              "partner" -> partner
            )
            None
        }
      case None =>
        logger.error("failedToRetrieveUsername")(
          "user" -> user,
          "partner" -> partner
        )
        None
    }
  }
}
