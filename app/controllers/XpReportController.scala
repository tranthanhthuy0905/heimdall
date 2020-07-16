package controllers

import actions.{HeimdallRequestAction, PermValidationActionBuilder, XpReportRequestAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.logging.Logger.LogVariable
import javax.inject.Inject
import models.common.{FileIdent, PermissionType}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Results}
import services.xpreport.playback.{EventsInfo, PlaybackJsonConversions, StalledInfo, Token, Time}

import scala.concurrent.{ExecutionContext, Future}

class XpReportController @Inject()(
                                    heimdallRequestAction: HeimdallRequestAction,
                                    permValidation: PermValidationActionBuilder,
                                    xpReportRequestAction: XpReportRequestAction,
                                    components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with PlaybackJsonConversions
    with LazyLogging {

  def logInfo: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        xpReportRequestAction
      ).async { request =>

      val eventsInfo = playbackInfoFromJson(request.body.asJson.get)
      logLagRatioImpl(request.file, eventsInfo)
      Future.successful(Results.Ok)
    }

  def logStalled: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        xpReportRequestAction
    ).async { request =>

      val stalledInfo = stalledInfoFromJson(request.body.asJson.get)
      logStalledImpl(request.file, stalledInfo)
      Future.successful(Results.Ok)
    }

  private def logLagRatioImpl(fileIdent: FileIdent, eventsInfo: EventsInfo): Unit = {
    val time = eventsInfo.time match {
      case Some(Time(Some(time))) => time
      case _ => "unknown"
    }
    val streamToken = eventsInfo.token match {
      case Some(Token(Some(token))) => token
      case _ => "unknown"
    }
    val logVars: Seq[LogVariable] = Seq(
      "event_type" -> "info",
      "event_time" -> time,
      "evidence_id" -> fileIdent.evidenceId,
      "partner_id" -> fileIdent.partnerId,
      "file_id" -> fileIdent.fileId,
      "stream_token" -> streamToken,
      "events_info" -> eventsInfo,
    ) ++ calculateLagRatio(eventsInfo)

    logger.info("ExperienceReport")(logVars: _*)
  }

  private def logStalledImpl(fileIdent: FileIdent, stalledInfo: StalledInfo): Unit = {
    val time = stalledInfo.time match {
      case Some(Time(Some(time))) => time
      case _ => "unknown"
    }
    val currentResolution = stalledInfo
      .data
      .map(_.buffering.currentResolution)
      .flatMap(_.value).getOrElse(-1)

    logger.info("ExperienceReport")(
      "event_type" -> "stalled",
      "event_time" -> time,
      "evidence_id" -> fileIdent.evidenceId,
      "partner_id" -> fileIdent.partnerId,
      "file_id" -> fileIdent.fileId,
      "stalled_info" -> stalledInfo,
      "buffering_resolution" -> currentResolution,
    )
  }
}
