package controllers

import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.auth.{AuthorizationAttr, StreamingSessionData}
import models.play.HeimdallActionBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions, EvidenceRecordBufferedEvent}
import services.rtm.RtmClient

import scala.collection.SortedSet
import scala.concurrent.{ExecutionContext, Future}

class ProbeController @Inject()(action: HeimdallActionBuilder,
                                rtm: RtmClient,
                                sessionData: StreamingSessionData,
                                audit: AuditClient,
                                config: Config,
                                components: ControllerComponents)
                               (implicit ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging with AuditConversions {

  def probe: Action[AnyContent] = action.async { implicit request =>
    rtm.send(request.rtmQuery).flatMap { response =>
      if (response.status == OK) {
        val authHandler = request.attrs(AuthorizationAttr.Key)

        val streamingSessionToken = sessionData.createStreamingSessionToken(authHandler.token, SortedSet(request.rtmQuery.file.fileId))

        val auditEvent = EvidenceRecordBufferedEvent(
          evidenceTid(request.rtmQuery.file),
          updatedByTid(authHandler.jwt),
          fileTid(request.rtmQuery.file),
          request.remoteAddress)

        val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("application/json")

        audit.recordEndSuccess(auditEvent).map { _ =>
          val responseWithToken = response.json.as[JsObject] + ("streamingSessionToken" -> Json.toJson(streamingSessionToken))
          Ok(responseWithToken).as(contentType)
        } recoverWith {
          case exception: Exception =>
            logger.error(exception, "failedToSendProbeAuditEvent")("exception" -> exception.getMessage)
            Future.successful(InternalServerError(Json.obj("exception" -> exception.getMessage)))
        }
      } else {
        logger.error(s"unexpectedProbeRequestReturnCode")("status" -> response.status)
        Future.successful(InternalServerError)
      }
    }
  }

}
