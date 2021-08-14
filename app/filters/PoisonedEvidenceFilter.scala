package filters
import java.util.UUID

import akka.stream.Materializer
import com.typesafe.config.Config
import javax.inject.Inject
import play.api.mvc.{Filter, RequestHeader, Result, Results}
import utils.UUIDHelper

import scala.concurrent.{ExecutionContext, Future}

class PoisonedEvidenceFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext, config: Config)
  extends Filter
    with UUIDHelper {

  final val poisonedEvidenceList = config.getStringList("heimdall.poisoned_evidences")


  def apply(
             nextFilter: RequestHeader => Future[Result]
           )(requestHeader: RequestHeader): Future[Result] = {

    getUuidListByKey("evidence_id", requestHeader.queryString) match {
      case Some(evidenceIds) =>
        if (filterPoisonedEvidence(evidenceIds)) {
          Future.successful(Results.BadRequest)
        } else {
          nextFilter(requestHeader)
        }
      case _ => nextFilter(requestHeader)
    }
  }

  private def filterPoisonedEvidence(evidenceIds: List[UUID]): Boolean = {
    evidenceIds.exists(evidenceId => poisonedEvidenceList.contains(evidenceId.toString.toLowerCase))
  }
}
