package controllers

import actions._
import akka.util.ByteString
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.{ContentTypes, HttpEntity}
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, ResponseHeader, Result}

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

      audit.recordEndSuccess(
          EvidencePlaybackRequested(
            evidenceTid(request.file.evidenceId, request.file.partnerId),
            updatedByTid(authHandler.parsedJwt),
            fileTid(request.file.fileId, request.file.partnerId),
            request.request.clientIpAddress))

      apidae.transcode(request.file.partnerId, request.userId, request.file.evidenceId, request.file.fileId)
    }

  def status: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
    ).async { implicit request =>
        apidae.getTranscodingStatus(request.file.partnerId, request.userId, request.file.evidenceId, request.file.fileId)
    }

  implicit def Response2Result(response: Future[WSResponse]): Future[Result] = {
    response map {
      response =>
        val headers = response.headers map {
          h => (h._1, h._2.head)
        }

        val body = HttpEntity.Strict(ByteString(response.body), Some(ContentTypes.JSON))
        Result(ResponseHeader(response.status, headers), body)
    }
  }
}
