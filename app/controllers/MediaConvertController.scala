package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import services.apidae.ApidaeClient
import services.audit.{AuditClient, AuditConversions, EvidencePlaybackRequested}
import services.rti.metadata.MetadataJsonConversions

class MediaConvertController @Inject()(
                                        heimdallRequestAction: HeimdallRequestAction,
                                        tokenValidationAction: TokenValidationAction,
                                        permValidation: PermValidationActionBuilder,
                                        apidaeRequestAction: ApidaeRequestAction,
                                        apidae: ApidaeClient,
                                        audit: AuditClient,
                                        components: ControllerComponents)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with MetadataJsonConversions {

  def convert: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
    ).async { implicit request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)

      for {
        response <- apidae
          .transcode(request.file.partnerId, request.userId, request.file.evidenceId, request.file.fileId)
        _ <- audit.recordEndSuccess(
          EvidencePlaybackRequested(
            evidenceTid(request.file.evidenceId, request.file.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(request.file.fileId, request.file.partnerId),
            request.request.clientIpAddress
          ))
        httpEntity <- Future.successful(toHttpEntity(response))
      } yield httpEntity.fold(InternalServerError(_), Ok(_))
    }

  def status: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
    ).async { implicit request =>
      for {
        response <- apidae
          .getTranscodingStatus(request.file.partnerId, request.userId, request.file.evidenceId, request.file.fileId)
        httpEntity <- Future.successful(toHttpEntity(response))
      } yield httpEntity.fold(BadRequest(_), Ok(_))
    }

  private def toHttpEntity(response: WSResponse): Either[JsObject, JsValue] = {
    Some(response.status)
      .filter(_ equals play.api.http.Status.OK)
      .toRight(Json.obj("message" -> response.body))
      .map(_ => response.body[JsValue])
  }
}
