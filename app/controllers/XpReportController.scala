package controllers

import actions.{HeimdallRequestAction, PermValidationActionBuilder, XpReportRequestAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.logging.Logger.LogVariable
import javax.inject.Inject
import models.common.{FileIdent, PermissionType}
import play.api.libs.json.{JsBoolean, Json}
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

      request.body.asJson.map { json =>
        val eventsInfo = playbackInfoFromJson(json)
        logLagRatioImpl(request.file, eventsInfo)
        Future.successful(Results.Ok(JsBoolean(true)))
      }.getOrElse{
        Future.successful(Results.BadRequest(Json.obj("error" -> "Empty body, Have a nice day")))
      }
    }

  def logStalled: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        xpReportRequestAction
    ).async { request =>
      request.body.asJson.map { json =>
        val stalledInfo = stalledInfoFromJson(json)
        logStalledImpl(request.file, stalledInfo)
        Future.successful(Results.Ok(JsBoolean(true)))
      }.getOrElse{
        Future.successful(Results.BadRequest(Json.obj("error" -> "Empty body, Have a nice day")))
      }
    }

  private def logLagRatioImpl(fileIdent: FileIdent, eventsInfo: EventsInfo): Unit = {
    val logVars: Seq[LogVariable] = Seq(
      "event_type" -> "info",
      "evidence_id" -> fileIdent.evidenceId,
      "partner_id" -> fileIdent.partnerId,
      "file_id" -> fileIdent.fileId,
      "events_info" -> eventsInfo,
    ) ++ logEventsInfoDetail(eventsInfo)

    logger.info("ExperienceReport")(logVars: _*)
  }

  private def logStalledImpl(fileIdent: FileIdent, stalledInfo: StalledInfo): Unit = {
    val logVars: Seq[LogVariable] = Seq(
      "event_type" -> "stalled",
      "evidence_id" -> fileIdent.evidenceId,
      "partner_id" -> fileIdent.partnerId,
      "file_id" -> fileIdent.fileId,
      "stalled_info" -> stalledInfo,
    ) ++ logStalledInfoDetail(stalledInfo)

    logger.info("ExperienceReport")(logVars: _*)
  }
}
