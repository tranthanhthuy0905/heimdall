package controllers.v1

import actions.{HeimdallRequestAction, PermValidationActionBuilder, RtmRequestAction}
import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.common.{AuthorizationAttr, PermissionType}
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.audit.{AuditClient, AuditConversions, EvidenceFileBookmarkDownloadedEvent}
import services.rtm.{RtmClient, RtmResponseHandler}
import utils.RequestUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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
    with AuditConversions {

  def downloadThumbnail: Action[AnyContent] =
    (
      heimdallRequestAction andThen
        permValidation.build(PermissionType.View) andThen
        rtmRequestAction
    ).async { request =>
      val authHandler = request.attrs(AuthorizationAttr.Key)

      val okCallback = (response: WSResponse) => {

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

        val contentType = response.headers
          .get("Content-Type")
          .flatMap(_.headOption)
          .getOrElse("image/jpeg")

        Try(audit.recordEndSuccess(auditEvents)) match {
          case Success(value) =>
            value.map { _ =>
              Ok.chunked(response.bodyAsSource).as(contentType)
            } recoverWith {
              case exception: Exception =>
                logger.error(
                  exception,
                  "failedToSendEvidenceFileBookmarkDownloadedEvent"
                )(
                  "exception"  -> exception.getMessage,
                  "path"       -> request.path,
                  "mediaIdent" -> request.media,
                  "token"      -> request.streamingSessionToken
                )
                Future.successful(InternalServerError)
            }
          case Failure(exception) =>
            logger.error(exception, "exceptionDuringEngagingAuditClient")(
              "exception"  -> exception.getMessage,
              "path"       -> request.path,
              "mediaIdent" -> request.media,
              "token"      -> request.streamingSessionToken
            )
            Future.successful(InternalServerError)
        }
      }

      rtm.send(request) flatMap { response =>
        RtmResponseHandler(
          response,
          okCallback,
          Seq[(String, Any)](
            "path"       -> request.path,
            "mediaIdent" -> request.media,
            "status"     -> response.status,
            "message"    -> response.body,
            "token"      -> request.streamingSessionToken
          )
        )
      }
    }

}
