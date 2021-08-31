package actions

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging
import services.apidae.{ConcatenationFile, ConcatenationRequest}
import utils.UUIDHelper
import models.common.HeimdallRequest
import play.api.libs.json.JsValue
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ConcatenationRequestAction @Inject()()(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, ConcatenationRequest]
    with LazyLogging
    with UUIDHelper {

  def refine[A](request: HeimdallRequest[A]): Future[Either[Results.Status, ConcatenationRequest[A]]] = {
    Future.successful(
      bodyJson(request.body)
        .flatMap(json => {
          val groupId = (json \ "group_id").asOpt[String].flatMap(Convert.tryToUuid)
          val caseIds = (json \ "case_ids").asOpt[Seq[String]].map(seq => seq.flatMap(s => Convert.tryToUuid(s)))

          for {
            userUUID    <- Convert.tryToUuid(request.subjectId)
            partnerUUID <- Convert.tryToUuid(request.audienceId)
            title       <- (json \ "title").asOpt[String]
            files       <- (json \ "files").asOpt[Seq[ConcatenationFile]]
          } yield ConcatenationRequest(partnerUUID, userUUID, title, files, groupId, caseIds, request)
        })
        .toRight(Results.BadRequest))
  }

  def bodyJson[A](body: A): Option[JsValue] = {
    body match {
      case js: JsValue     => Some(js)
      case any: AnyContent => any.asJson
      case _               => None
    }
  }
}
