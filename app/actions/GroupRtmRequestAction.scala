package actions

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import models.common.{HeimdallRequest, MediaIdent}
import play.api.mvc.{ActionRefiner, Results}
import services.komrade.KomradeClient
import services.rtm.{GroupRtmRequest, HeimdallRoutes, RtmQueryHelper, RtmRequest}
import services.url.PresignedUrlClient
import services.zookeeper.HeimdallLoadBalancer
import utils.EitherHelpers
import scala.collection.immutable.Seq

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class GroupRtmRequestAction @Inject()(
                                       config: Config,
                                       presignedUrlReq: PresignedUrlClient,
                                       komrade: KomradeClient,
                                       loadBalancer: HeimdallLoadBalancer,
                                     )(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[HeimdallRequest, GroupRtmRequest]
    with LazyLogging
    with HeimdallRoutes
    with EitherHelpers {

  def refine[A](input: HeimdallRequest[A]) = {
    val res = RtmQueryHelper(input.path, input.queryString).toRight(Results.BadRequest).map { rtmQuery =>
      Future.traverse(input.media.toList)(fileIdent => {
        for {
          presignedUrl <- presignedUrlReq.getUrl(fileIdent, input)
          endpoint <- loadBalancer.getInstanceAsFuture(fileIdent.fileId.toString)
          queries <- Future(RtmQueryHelper.getRTMQueries(rtmQuery.params, None, None, Seq(presignedUrl), input.audienceId))
        } yield {
          val uri = Uri.from(
            scheme = "https",
            host = endpoint.host,
            port = endpoint.port,
            path = rtmQuery.path,
            queryString = Some(queries)
          )
          val newMedia = new MediaIdent(
            fileIds = List(fileIdent.fileId),
            evidenceIds = List(fileIdent.evidenceId),
            partnerId = fileIdent.partnerId
          )
          new RtmRequest(uri, newMedia, Seq(presignedUrl), rtmQuery.params, input)
        }
      })
        .map(new GroupRtmRequest(_, input))
    }
    foldEitherOfFuture(res)
  }
}
