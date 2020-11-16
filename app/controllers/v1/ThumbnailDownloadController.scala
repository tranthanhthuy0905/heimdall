package controllers.v1

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.audit.{AuditClient, AuditConversions, EvidenceFileBookmarkDownloadedEvent}
import services.rtm.RtmClient
import utils.{HdlResponseHelpers, RequestUtils, WSResponseHelpers}

import scala.concurrent.ExecutionContext

class ThumbnailDownloadController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  rtmRequestAction: RtmRequestAction,
  rtm: RtmClient,
  audit: AuditClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with WSResponseHelpers
    with HdlResponseHelpers {

  def downloadThumbnail: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        rtmRequestAction
    ).async { request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)
      val auditEvents: List[EvidenceFileBookmarkDownloadedEvent] =
        request.media.toList.map(
          file =>
            EvidenceFileBookmarkDownloadedEvent(
              evidenceTid(file.evidenceId, file.partnerId),
              updatedByTid(authHandler.parsedJwt),
              fileTid(file.fileId, file.partnerId),
              RequestUtils.getClientIpAddress(request)
          )
        )
      (
        for {
          response <- FutureEither(rtm.send(request).map(withOKStatus))
          _ <- FutureEither(audit.recordEndSuccess(auditEvents))
            .mapLeft(toHttpStatus("failedToSendMediaViewedAuditEvent")(_, Some(request.media)))
        } yield response
      ).fold(error, streamed(_, "image/jpeg"))
    }
}
