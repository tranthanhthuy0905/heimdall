package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.{ContentTypes, HttpEntity}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, ResponseHeader, Result}

import scala.concurrent.{ExecutionContext, Future}
import services.apidae.ApidaeClient
import services.audit.{AuditClient, AuditConversions, AuditEvent, EvidencePlaybackRequested}
import services.rti.metadata.MetadataJsonConversions
import utils.WSResponseHelpers

class MediaConvertController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  featureValidationAction: FeatureValidationActionBuilder,
  apidaeRequestAction: ApidaeRequestAction,
  apidae: ApidaeClient,
  audit: AuditClient,
  config: Config,
  components: ControllerComponents)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with MetadataJsonConversions
    with WSResponseHelpers {

  def convert: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen featureValidationAction.build("edc.service.apidae.enable")
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
    ).async { implicit request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)
      val playbackRequestedEvent = EvidencePlaybackRequested(
        evidenceTid(request.file.evidenceId, request.file.partnerId),
        updatedByTid(authHandler.parsedJwt),
        fileTid(request.file.fileId, request.file.partnerId),
        request.request.clientIpAddress
      )

      logAuditEvent(playbackRequestedEvent)

      FutureEither(
        apidae
          .transcode(request.file.partnerId, request.userId, request.file.evidenceId, request.file.fileId)
          .map(withOKStatus))
        .fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), response => Ok(response.body).as(ContentTypes.JSON))
    }

  def status: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen featureValidationAction.build("edc.service.apidae.enable")
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
    ).async { implicit request =>
      FutureEither(
        apidae
          .getTranscodingStatus(request.file.partnerId, request.userId, request.file.evidenceId, request.file.fileId)
          .map(withOKStatus))
        .fold(l => Result(ResponseHeader(l), HttpEntity.NoEntity), response => Ok(response.body).as(ContentTypes.JSON))
    }

  private def logAuditEvent(event: AuditEvent): Future[Either[Int, String]] = {
    audit.recordEndSuccess(event).map(Right(_)).recover { case _ => Left(INTERNAL_SERVER_ERROR) }
  }
}
