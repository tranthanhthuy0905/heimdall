package actions

import akka.http.scaladsl.model.Uri
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Results}
import services.dredd.DreddClient
import services.komrade.KomradeClient
import services.rtm.{GroupRtmRequest, HeimdallRoutes, RtmQueryHelper, RtmRequest}
import services.zookeeper.HeimdallLoadBalancer

import scala.concurrent.{ExecutionContext, Future}

case class GroupRtmRequestAction @Inject()(
                                       config: Config,
                                       dredd: DreddClient,
                                       komrade: KomradeClient,
                                       loadBalancer: HeimdallLoadBalancer,
                                     )(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[HeimdallRequest, GroupRtmRequest]
    with LazyLogging
    with HeimdallRoutes {

  def refine[A](input: HeimdallRequest[A]) = {
    RtmQueryHelper(input.path, input.queryString).map { rtmQuery =>
      for {
        presignedUrls <- Future.traverse(input.media.toList)(dredd.getUrl(_, input))
        endpoints      <- Future.traverse(input.media.toList)(media => loadBalancer.getInstanceAsFuture(media.fileId.toString))
        queries       <- Future(RtmQueryHelper.getRTMQueries(rtmQuery.params, None, None, presignedUrls, input.audienceId))
      } yield {
        val reqs = endpoints.map { endpoint =>
          Uri.from(
            scheme = "https",
            host = endpoint.host,
            port = endpoint.port,
            path = rtmQuery.path,
            queryString = Some(queries)
          )
        }
          .map { uri => new RtmRequest(uri, input)}
         Right(new GroupRtmRequest(reqs))
      }
    }.getOrElse(Future.successful(Left(Results.BadRequest)))
  }


}
