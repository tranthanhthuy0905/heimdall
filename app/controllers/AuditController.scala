package controllers

import com.evidence.service.common.logging.LazyLogging
import javax.inject.Inject
import models.auth.AuthorizationAttr
import models.common.HeimdallActionBuilder
import play.api.libs.json.Json
import play.api.mvc._
import services.audit.{AuditClient, AuditConversions, EvidenceFileStreamedEvent}
import utils.RequestUtils

import scala.concurrent.{ExecutionContext, Future}

class AuditController @Inject()(action: HeimdallActionBuilder,
                                audit: AuditClient,
                                components: ControllerComponents)
                               (implicit ex: ExecutionContext)
  extends AbstractController(components) with LazyLogging with AuditConversions {

  def recordMediaStreamedEvent: Action[AnyContent] = action.async { implicit request =>
    val authHandler = request.attrs(AuthorizationAttr.Key)

    val auditEvents: List[EvidenceFileStreamedEvent] =
      request.rtmQuery.media.toList.map(file => EvidenceFileStreamedEvent(
        evidenceTid(file.evidenceId, file.partnerId),
        updatedByTid(authHandler.jwt),
        fileTid(file.fileId, file.partnerId),
        RequestUtils.getClientIpAddress(request)
      ))

    audit.recordEndSuccess(auditEvents).map { _ =>
      Ok(Json.obj("status" -> "ok"))
    } recoverWith {
      case exception: Exception =>
        logger.error(exception, "failedToSendMediaStreamedAuditEvent")("exception" -> exception.getMessage)
        Future.successful(InternalServerError)
    }
  }
}
