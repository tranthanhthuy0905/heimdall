package controllers

import actions.{HeimdallRequestAction, XpReportRequestAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.logging.Logger.LogVariable
import models.auth.StreamingSessionData
import scala.collection.immutable.Seq

import javax.inject.Inject
import models.common.{FileIdent, PermissionType}
import play.api.libs.json.{JsBoolean, JsValue}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import services.xpreport.playback.{EventsInfo, PlaybackJsonConversions, StalledData, StalledInfo, XpReportRequest}

import scala.concurrent.ExecutionContext

class XpReportController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  xpReportRequestAction: XpReportRequestAction,
  sessionData: StreamingSessionData,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with PlaybackJsonConversions
    with LazyLogging {

  def logInfo: Action[JsValue] =
    (
      heimdallRequestAction andThen
        xpReportRequestAction
    ).apply(parse.json) { request =>
      if (!isValid(request)) Forbidden(JsBoolean(false))
      else {
        val json       = request.body
        val eventsInfo = playbackInfoFromJson(json)
        logLagRatioImpl(request.file, eventsInfo)
        Ok(JsBoolean(true))
      }
    }

  def logStalled: Action[JsValue] =
    (
      heimdallRequestAction andThen
        xpReportRequestAction
    ).apply(parse.json) { request =>
      if (!isValid(request)) Forbidden(JsBoolean(false))
      else {
        val json        = request.body
        val stalledInfo = stalledInfoFromJson(json)
        logStalledImpl(request.file, stalledInfo)
        Ok(JsBoolean(true))
      }
    }

  private def isValid(request: XpReportRequest[JsValue]): Boolean = {
    val token = (request.body \ "token").asOpt[String].getOrElse("")
    sessionData.validateToken(token, request.request.cookie, request.request.media.getSortedFileIds)
  }

  private def logLagRatioImpl(fileIdent: FileIdent, eventsInfo: EventsInfo): Unit = {
    val logVars: Seq[LogVariable] = Seq(
      "event_type"  -> "info",
      "evidence_id" -> fileIdent.evidenceId,
      "partner_id"  -> fileIdent.partnerId,
      "file_id"     -> fileIdent.fileId,
    ) ++ logEventsInfoDetail(eventsInfo)

    logger.info("ExperienceReport")(logVars: _*)
  }

  private def logStalledImpl(fileIdent: FileIdent, stalledInfo: StalledInfo): Unit = {
    val event = stalledInfo.event.flatMap(_.value).getOrElse("START")
    stalledInfo.data match {
      case Some(StalledData(_, _, _, _, buffering)) =>
        // Log only when buffering duration larger than 2s
        if (event != "END" || (event == "END" && buffering.stalledDuration.flatMap(_.value).getOrElse(0.0) >= 2000)) {
          val logVars: Seq[LogVariable] = Seq(
            "event_type"  -> "stalled",
            "evidence_id" -> fileIdent.evidenceId,
            "partner_id"  -> fileIdent.partnerId,
            "file_id"     -> fileIdent.fileId,
          ) ++ logStalledInfoDetail(stalledInfo)

          logger.info("ExperienceReport")(logVars: _*)
        }
      case None => None
    }
  }
}
