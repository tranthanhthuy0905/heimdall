package controllers

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction, TokenValidationAction, WatermarkAction}
import akka.stream.scaladsl.Source
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{HeimdallRtiActionBuilder, PermissionType}
import play.api.http.{HttpChunk, HttpEntity}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, ExecutionException, Future}
import services.audit.{AuditClient, AuditConversions, EvidenceViewed}
import services.dredd.DreddClient
import services.rti.RtiClient
import utils.RequestUtils

class ImageController @Inject()(heimdallRequestAction: HeimdallRequestAction,
                                tokenValidationAction: TokenValidationAction,
                                permValidation: PermValidationActionBuilder,
                                watermarkAction: WatermarkAction,
                                action: HeimdallRtiActionBuilder,
                                rti: RtiClient,
                                audit: AuditClient,
                                components: ControllerComponents)
                               (implicit ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging with AuditConversions {

    def view: Action[AnyContent] = ???

//  def view: Action[AnyContent] = (
//    heimdallRequestAction andThen
//      permValidation.build(PermissionType.View) andThen
//      watermarkAction
//    ).async { implicit request =>
//      val authHandler = request.attrs(AuthorizationAttr.Key)
//      val sizeID = request.getQueryString("size_id")
//      val image = request.rtiQuery.image
//      for {
//        watermark <- watermarkProvider.createWatermarkForRti(authHandler.jwt)
//        response <- rti.getImage(sizeID.get, presignedUrl.toString, watermark.get)
//        _ <- audit.recordEndSuccess(EvidenceViewed(
//          evidenceTid(image.evidenceId, image.partnerId),
//          updatedByTid(authHandler.jwt),
//          fileTid(image.fileId, image.partnerId),
//          RequestUtils.getClientIpAddress(request)
//        ))
//        httpEntity <- Future.successful(toHttpEntity(response))
//      } yield httpEntity.fold(
//        BadRequest(_),
//        entity => {
//          Ok.sendEntity(entity)
//        })
//    }

  private def toHttpEntity(response: WSResponse): Either[JsObject, HttpEntity] = {
    Some(response.status)
      .filter(_ equals play.api.http.Status.OK)
      .toRight(Json.obj("message" -> response.body))
      .map(_ => {
        val contentType = response.headers.getOrElse("Content-Type", Seq("image/jpeg")).headOption

        response.headers.get("Content-Length")
          .map(length => HttpEntity.Streamed(response.bodyAsSource, Some(length.mkString.toLong), contentType))
          .getOrElse(HttpEntity.Chunked(Source.apply(List(HttpChunk.Chunk(response.bodyAsBytes))), contentType))
      })
  }
}