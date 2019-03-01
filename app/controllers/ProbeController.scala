package controllers

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.auth.{AuthorizationAttr, StreamingSessionData}
import models.common.HeimdallActionBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions, EvidenceRecordBufferedEvent}
import services.nino.{NinoClient, NinoClientAction}
import services.rtm.RtmClient

import scala.concurrent.{ExecutionContext, Future}

class ProbeController @Inject()(action: HeimdallActionBuilder,
                                rtm: RtmClient,
                                sessionData: StreamingSessionData,
                                audit: AuditClient,
                                nino: NinoClient,
                                components: ControllerComponents)
                               (implicit ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging with AuditConversions {

  def probe: Action[AnyContent] = action.async { implicit request =>
    val authHandler = request.attrs(AuthorizationAttr.Key)
    val rtmResponse = for {
      // TODO: refactor actions and move the access logic into a separate action builder.
      // TODO: this can be done after performance requirements are determined and met.
      accessResult <- nino.enforce(authHandler.jwtString, request.rtmQuery.media.toEvidenceEntityDescriptors, NinoClientAction.View)
      _ <- utils.Predicate(accessResult)(new Exception(s"media [${request.rtmQuery.media}] does not have ${NinoClientAction.View} access"))
      response <- rtm.send(request.rtmQuery)
    } yield response

    rtmResponse flatMap { response =>
      if (response.status == OK) {
        val streamingSessionToken = sessionData.createToken(authHandler.token, request.rtmQuery.media.getSortedFileIds)

        val auditEvents: List[EvidenceRecordBufferedEvent] =
          request.rtmQuery.media.toList.map(file => EvidenceRecordBufferedEvent(
            evidenceTid(file.evidenceId, file.partnerId),
            updatedByTid(authHandler.jwt),
            fileTid(file.fileId, file.partnerId),
            request.remoteAddress
          ))

        val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("application/json")
        audit.recordEndSuccess(auditEvents).map { _ =>
          val responseWithToken = response.json.as[JsObject] + ("streamingSessionToken" -> Json.toJson(streamingSessionToken))
          Ok(responseWithToken).as(contentType)
        } recoverWith {
          case exception: Exception =>
            logger.error(exception, "failedToSendProbeAuditEvent")("exception" -> exception.getMessage)
            Future.successful(InternalServerError(Json.obj("exception" -> exception.getMessage)))
        }
      } else {
        logger.error("unexpectedProbeRequestReturnCode")("status" -> response.status)
        Future.successful(InternalServerError)
      }
    }
  }

}
