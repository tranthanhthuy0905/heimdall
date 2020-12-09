package controllers

import actions._
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.ContentTypes
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext
import services.apidae.ApidaeClient
import services.audit.{AuditClient, AuditConversions, EvidencePlaybackRequested}
import services.rti.metadata.MetadataJsonConversions
import utils.{HdlResponseHelpers, WSResponseHelpers}

class MediaConvertController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  tokenValidationAction: TokenValidationAction,
  permValidation: PermValidationActionBuilder,
  featureValidationAction: FeatureValidationActionBuilder,
  apidaeRequestAction: ApidaeRequestAction,
  mediaConvertValidation: MediaConvertValidation,
  apidae: ApidaeClient,
  audit: AuditClient,
  config: Config,
  components: ControllerComponents)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with MetadataJsonConversions
    with WSResponseHelpers
    with HdlResponseHelpers {

  def convert: Action[AnyContent] =
    (
      heimdallRequestAction
        andThen featureValidationAction.build("edc.service.apidae.enable")
        andThen permValidation.build(PermissionType.View)
        andThen apidaeRequestAction
        andThen mediaConvertValidation
    ).async { implicit request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)
      val playbackRequestedEvent = EvidencePlaybackRequested(
        evidenceTid(request.file.evidenceId, request.file.partnerId),
        updatedByTid(authHandler.parsedJwt),
        fileTid(request.file.fileId, request.file.partnerId),
        request.request.clientIpAddress
      )

      FutureEither(audit.recordEndSuccess(playbackRequestedEvent))
        .mapLeft(toHttpStatus("failedToSendEvidencePlaybackRequestedAuditEvent")(_))

      FutureEither(
        apidae
          .transcode(request.file.partnerId, request.userId, request.file.evidenceId, request.file.fileId)
          .map(withOKStatus))
        .fold(error, response => Ok(response.json).as(ContentTypes.JSON))
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
        .fold(error, response => Ok(response.json).as(ContentTypes.JSON))
    }
}
