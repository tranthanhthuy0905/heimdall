package controllers

import actions.{HeimdallRequestAction, PartnerPermValidationActionBuilder}
import com.evidence.service.komrade.thrift.{WatermarkPosition, WatermarkSetting}
import javax.inject.Inject
import models.common.PermissionType
import play.api.libs.json.{JsObject, JsResultException, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.komrade.KomradeClient

import scala.concurrent.{ExecutionContext, Future}

class PartnerController  @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  komrade: KomradeClient,
  partnerPermValidationActionBuilder: PartnerPermValidationActionBuilder,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
  extends AbstractController(components) {

  def getWatermarkSettings(partnerId: String): Action[AnyContent] = {
    (
      heimdallRequestAction
        andThen partnerPermValidationActionBuilder.build(PermissionType.PartnerAnyRead)
    ) async { _ =>
      komrade.getWatermarkSettings(partnerId)
        .map(settings => Ok(watermarkSettingToJson(settings)))
    }
  }

  def updateWatermarkSettings(partnerId: String): Action[AnyContent] = {
    (
      heimdallRequestAction
        andThen partnerPermValidationActionBuilder.build(PermissionType.PartnerEdit)
    ) async { request =>
      request.body.asJson
        .map{json => {
          try {
            val settings = jsonToWatermarkSetting(json)
            komrade.updateWatermarkSettings(partnerId, settings)
              .map(_ => Ok(Json.obj("status" -> "ok")))
          } catch {
            case _: JsResultException => Future.successful(BadRequest("Invalid json request"))
          }
        }}
        .getOrElse(Future.successful(BadRequest("Expecting json body")))
    }
  }

  private def watermarkSettingToJson(settings: WatermarkSetting): JsObject = {
    Json.obj(
      "data" -> Json.obj(
        "watermarkSettings" -> Json.obj(
          "partnerId" -> settings.partnerId,
          "position" -> settings.position.value
        )
      )
    )
  }

  private def jsonToWatermarkSetting(json: JsValue): WatermarkSetting = {
    val partnerId = (json \ "data" \ "watermarkSettings" \ "partnerId").get.as[String]
    val positionValue = (json \ "data" \ "watermarkSettings" \ "position").get.as[Int]
    WatermarkSetting(partnerId, WatermarkPosition(positionValue))
  }
}
