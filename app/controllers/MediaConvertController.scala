package controllers

import actions._
import akka.util.ByteString
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.{ContentTypes, HttpEntity}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, ResponseHeader, Result}

import scala.concurrent.{ExecutionContext, Future}
import services.apidae.ApidaeClient
import services.audit.{AuditClient, AuditConversions, EvidencePlaybackRequested}
import services.rti.metadata.MetadataJsonConversions
import utils.ResponseHeaderHelpers

class MediaConvertController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  apidaeRequestAction: ApidaeRequestAction,
  apidae: ApidaeClient,
  audit: AuditClient,
  config: Config,
  components: ControllerComponents)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with MetadataJsonConversions
    with ResponseHeaderHelpers {

  def convert: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
    ).async { implicit request =>
      val isApidaeEnable = config.getBoolean("edc.service.apidae.enable")
      if (!isApidaeEnable) {
        Future.successful(NotImplemented(Json.obj("message" -> "This function is under-construction for your region")))
      } else {
        val authHandler = request.attrs(AuthorizationAttr.Key)

        audit.recordEndSuccess(
          EvidencePlaybackRequested(
            evidenceTid(request.file.evidenceId, request.file.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(request.file.fileId, request.file.partnerId),
            request.request.clientIpAddress
          ))

        apidae.transcode(request.file.partnerId, request.userId, request.file.evidenceId, request.file.fileId)
      }
    }

  def status: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
    ).async { implicit request =>
      val isApidaeEnable = config.getBoolean("edc.service.apidae.enable")
      if (!isApidaeEnable) {
        Future.successful(NotImplemented(Json.obj("message" -> "This function is under-construction for your region")))
      } else {
        apidae
          .getTranscodingStatus(request.file.partnerId, request.userId, request.file.evidenceId, request.file.fileId)
      }
    }

  implicit def Response2Result(response: Future[WSResponse]): Future[Result] = {
    response map { response =>
      val body = HttpEntity.Strict(ByteString(response.body), Some(ContentTypes.JSON))
      Result(ResponseHeader(response.status, withHeader(response.headers)), body)
    }
  }
}
