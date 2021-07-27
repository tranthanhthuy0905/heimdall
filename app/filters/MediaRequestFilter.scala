package filters

import akka.stream.Materializer
import javax.inject.Inject
import models.auth.Authorizer
import models.common.{MediaIdent, MediaIdentAttr}
import play.api.mvc.{Filter, RequestHeader, Result}
import utils.UUIDHelper

import scala.concurrent.{ExecutionContext, Future}

/** MediaRequestFilter performs the following actions:
  * - Extraction of required query parameters.
  *   - Extract required query parameters from the queryString as UUIDs (media identifiers - MediaIdent).
  *   - Store MediaIdent as request attribute.
  */
class MediaRequestFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext, authorizer: Authorizer)
    extends Filter
    with UUIDHelper {


  def apply(
    nextFilter: RequestHeader => Future[Result]
  )(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(this.getMediaIdent(requestHeader).map(requestHeader.addAttr(MediaIdentAttr.Key, _)).getOrElse(requestHeader))
  }

  private def getMediaIdent(
                             requestHeader: RequestHeader
                           ): Option[MediaIdent] = {
    for {
      fileIds     <- getUuidListByKey("file_id", requestHeader.queryString)
      evidenceIds <- getUuidListByKey("evidence_id", requestHeader.queryString)
      partnerId   <- getUuidValueByKey("partner_id", requestHeader.queryString)
    } yield new MediaIdent(fileIds, evidenceIds, partnerId)
  }
}
