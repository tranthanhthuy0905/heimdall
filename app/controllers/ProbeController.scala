package controllers

import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.auth.AuthorizationAttr
import models.play.HeimdallActionBuilder
import play.api.libs.json.Json
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions, EvidenceRecordBufferedEvent}
import services.rtm.{RtmClient, RtmRequestRoutes}

import scala.concurrent.{ExecutionContext, Future}

class ProbeController @Inject()(action: HeimdallActionBuilder,
                                rtm: RtmClient,
                                audit: AuditClient,
                                config: Config,
                                components: ControllerComponents)
                               (implicit ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging with RtmRequestRoutes with AuditConversions {

  def probe: Action[AnyContent] = action.async { implicit request =>
    rtm.send(Probe, request.validatedQuery).flatMap { response =>
      if (response.status == OK) {
        val authHandler = request.attrs(AuthorizationAttr.Key)

        // TODO: Generate stream ing token and add to the response.
        // TODO: Generate a secret and use session data of the sessions service to store it.
        // TODO: streaming session token -> HMAC(secret + sorted fileId-s).

        val auditEvent = EvidenceRecordBufferedEvent(
          evidenceTid(request.validatedQuery.file),
          updatedByTid(authHandler.jwt),
          fileTid(request.validatedQuery.file),
          request.remoteAddress)

        val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("application/json")

        audit.recordEndSuccess(auditEvent).map { _ =>
          Ok(response.json).as(contentType)
        } recoverWith {
          case exception: Exception =>
            logger.error(exception, "failedToSendAuditEvent")("exception" -> exception.getMessage)
            Future.successful(InternalServerError(Json.obj( "exception" -> exception.getMessage)))
        }
      } else {
        logger.error(s"unexpectedProbeRequestReturnCode")("status" -> response.status)
        Future.successful(InternalServerError)
      }
    }
  }

}
