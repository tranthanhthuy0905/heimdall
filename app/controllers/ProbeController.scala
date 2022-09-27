package controllers

import actions.{
  GroupRtmRequestAction,
  GroupRtmRequestFilterAction,
  HeimdallRequestAction,
  PermValidationActionBuilder,
  RtmRequestAction
}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.monad.FutureEither

import javax.inject.Inject
import models.auth.StreamingSessionData
import models.common.{AuthorizationAttr, PermissionType}
import play.api.http.ContentTypes
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions}
import services.rtm.{RtmClient, RtmRequest}
import services.sage.SageClient
import services.apidae.ApidaeClient
import utils.{AuditEventHelpers, EitherHelpers, HdlResponseHelpers, RequestUtils, WSResponseHelpers}

import scala.concurrent.{ExecutionContext, Future}

class ProbeController @Inject()(
  heimdallRequestAction: HeimdallRequestAction,
  permValidation: PermValidationActionBuilder,
  rtmRequestAction: RtmRequestAction,
  groupRtmRequestAction: GroupRtmRequestAction,
  groupRtmRequestFilterAction: GroupRtmRequestFilterAction,
  rtm: RtmClient,
  sessionData: StreamingSessionData,
  audit: AuditClient,
  apidae: ApidaeClient,
  sage: SageClient,
  components: ControllerComponents
)(implicit ex: ExecutionContext)
    extends AbstractController(components)
    with LazyLogging
    with AuditConversions
    with WSResponseHelpers
    with HdlResponseHelpers
    with AuditEventHelpers
    with EitherHelpers {

  def probe: Action[AnyContent] =
    (heimdallRequestAction andThen permValidation.build(PermissionType.View) andThen rtmRequestAction).async {
      request =>
        val authHandler   = request.authorizationData
        val updatedBy     = updatedByTid(authHandler.parsedJwt)
        val remoteAddress = RequestUtils.getClientIpAddress(request)
        val media         = request.media
        (
          for {
            // audit zip file accessed event
            zipAccessEvents <- FutureEither(
              Future
                .traverse(media.toList)(
                  file => getZipInfoAndBuildZipAccessEvent(sage, apidae, file, updatedBy, remoteAddress).future
                )
                .map(toEitherOfList)).map(list => list.flatten)
            _ <- FutureEither(audit.recordEndSuccessDebounce(zipAccessEvents))
              .mapLeft(toHttpStatus("failedToSendZipFileAccessedAuditEvent")(_, Some(request.media)))

            response <- FutureEither(rtm.probe(request).map(withOKStatus))
            _ <- FutureEither(
              audit.recordEndSuccessDebounce(
                zipAccessedToBufferEvents(media.toList, zipAccessEvents, updatedBy, remoteAddress)))
              .mapLeft(toHttpStatus("failedToSendMedaBufferEvent")(_, Some(request.media)))
          } yield toProbeResult(response, request)
        ).fold(error, Ok(_).as(ContentTypes.JSON))
    }

  def probeAll: Action[AnyContent] =
    (heimdallRequestAction
      andThen permValidation.build(PermissionType.View)
      andThen groupRtmRequestAction
      andThen groupRtmRequestFilterAction).async { request =>
      Future
        .traverse(request.toList) { rtmRequest =>
          rtm
            .send(rtmRequest)
            .map(withOKStatus)
            .map(response => response.map(toProbeResult(_, rtmRequest)))
        }
        .map { res =>
          toEitherOfList(res.toList)
            .fold(
              error,
              res =>
                Ok(
                  Json.obj(("data", res))
              )
            )
        }
    }

  private def generateStreamingToken(request: RtmRequest[AnyContent]): String = {
    val authHandler           = request.attrs(AuthorizationAttr.Key)
    val streamingSessionToken = sessionData.createToken(authHandler.token, request.media.getSortedFileIds)
    streamingSessionToken
  }

  private def toProbeResult(response: WSResponse, request: RtmRequest[AnyContent]): JsObject = {
    val streamingToken = generateStreamingToken(request)

    val responseWithToken = response.json
      .as[JsObject] + ("streamingSessionToken" -> Json.toJson(streamingToken)) + ("fileIds" -> Json.toJson(
      request.media.fileIds))
    responseWithToken
  }
}
