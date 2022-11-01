package controllers

import actions.{HeimdallRequestAction, PartnerPermValidationActionBuilder}
import com.evidence.api.thrift.v1.TidEntities
import com.evidence.service.audit.Tid
import com.evidence.service.common.Convert
import com.evidence.service.common.monad.FutureEither
import com.evidence.service.komrade.thrift.{WatermarkPosition, WatermarkSetting}

import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.libs.json.{JsObject, JsResultException, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.audit.{AuditClient, AuditConversions, WatermarkSettingsUpdatedEvent}
import services.komrade.KomradeClient
import utils.HdlResponseHelpers

import scala.concurrent.{ExecutionContext, Future}

class PartnerController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  komrade: KomradeClient,
  partnerPermValidationActionBuilder: PartnerPermValidationActionBuilder,
  audit: AuditClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with AuditConversions
    with HdlResponseHelpers {

  def getWatermarkSettings(partnerId: String): Action[AnyContent] = {
    (
      heimdallRequestAction
        andThen partnerPermValidationActionBuilder.build(partnerId)
    ) async { request =>
      komrade
        .getWatermarkSettings(partnerId, Some(request.requestInfo))
        .map(settings => Ok(watermarkSettingToJson(settings)))
    }
  }

  def updateWatermarkSettings(partnerId: String): Action[AnyContent] = {
    (
      heimdallRequestAction
        andThen partnerPermValidationActionBuilder.build(partnerId)
    ) async { request =>
      request.body.asJson.map { json =>
        {
          try {
            val settings     = jsonToWatermarkSetting(json)
            val normalizedId = Some(normalizeUuid(partnerId))
            val authHandler  = request.attrs(AuthorizationAttr.Key)
            val auditEvent = WatermarkSettingsUpdatedEvent(
              Tid(TidEntities.Partner, normalizedId, normalizedId),
              updatedByTid(authHandler.parsedJwt),
              request.clientIpAddress,
              settings.position.value
            )
            (for {
              response <- FutureEither(
                komrade
                  .updateWatermarkSettings(partnerId, settings, Some(request.requestInfo))
                  .map(_ => Some(Ok(Json.obj("status" -> "ok"))).toRight(INTERNAL_SERVER_ERROR)))
              _ <- FutureEither(audit.recordEndSuccess(auditEvent))
                .mapLeft(toHttpStatus("failedToSendWatermarkSettingsUpdatedEvent")(_))
            } yield response).fold(error, res => res)
          } catch {
            case _: JsResultException => Future.successful(BadRequest("Invalid json request"))
          }
        }
      }.getOrElse(Future.successful(BadRequest("Expecting json body")))
    }
  }

  private def watermarkSettingToJson(settings: WatermarkSetting): JsObject = {
    Json.obj(
      "data" -> Json.obj(
        "watermarkSettings" -> Json.obj(
          "partnerId" -> settings.partnerId,
          "position"  -> settings.position.value
        )
      )
    )
  }

  private def jsonToWatermarkSetting(json: JsValue): WatermarkSetting = {
    val partnerId     = (json \ "data" \ "watermarkSettings" \ "partnerId").get.as[String]
    val positionValue = (json \ "data" \ "watermarkSettings" \ "position").get.as[Int]
    WatermarkSetting(partnerId, WatermarkPosition(positionValue))
  }

  private def normalizeUuid(uuidString: String): String = {
    Convert
      .tryToUuid(uuidString)
      .map(normalizedUuid => normalizedUuid.toString)
      .getOrElse(uuidString)
  }
}
