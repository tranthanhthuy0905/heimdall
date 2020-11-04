package controllers

import actions.{HeimdallRequestAction, PermValidationActionBuilder, XpReportRequestAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.logging.Logger.LogVariable
import javax.inject.Inject
import models.common.{FileIdent, PermissionType}
import play.api.libs.json.{JsBoolean, JsValue}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import services.xpreport.playback.{EventsInfo, PlaybackJsonConversions, StalledInfo}

import scala.concurrent.ExecutionContext

class XpReportController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  xpReportRequestAction: XpReportRequestAction,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with PlaybackJsonConversions
    with LazyLogging {

  def logInfo: Action[JsValue] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        xpReportRequestAction
    ).apply(parse.json) { request =>
      val json       = request.body
      val eventsInfo = playbackInfoFromJson(json)
      logLagRatioImpl(request.file, eventsInfo)
      Ok(JsBoolean(true))
    }

  def logStalled: Action[JsValue] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        xpReportRequestAction
    ).apply(parse.json) { request =>
      val json        = request.body
      val stalledInfo = stalledInfoFromJson(json)
      logStalledImpl(request.file, stalledInfo)
      Ok(JsBoolean(true))
    }

  private def logLagRatioImpl(fileIdent: FileIdent, eventsInfo: EventsInfo): Unit = {
    val logVars: Seq[LogVariable] = Seq(
      "event_type"  -> "info",
      "evidence_id" -> fileIdent.evidenceId,
      "partner_id"  -> fileIdent.partnerId,
      "file_id"     -> fileIdent.fileId,
      "events_info" -> eventsInfo,
    ) ++ logEventsInfoDetail(eventsInfo)

    logger.info("ExperienceReport")(logVars: _*)
  }

  private def logStalledImpl(fileIdent: FileIdent, stalledInfo: StalledInfo): Unit = {
    val logVars: Seq[LogVariable] = Seq(
      "event_type"   -> "stalled",
      "evidence_id"  -> fileIdent.evidenceId,
      "partner_id"   -> fileIdent.partnerId,
      "file_id"      -> fileIdent.fileId,
      "stalled_info" -> stalledInfo,
    ) ++ logStalledInfoDetail(stalledInfo)

    logger.info("ExperienceReport")(logVars: _*)
  }
}
