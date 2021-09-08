package actions

import com.typesafe.config.Config
import javax.inject.Inject
import play.api.mvc.{ActionFilter, Result, Results}
import services.rtm.GroupRtmRequest

import scala.concurrent.{ExecutionContext, Future}

case class GroupRtmRequestFilterAction @Inject()()(implicit val executionContext: ExecutionContext, config: Config)
  extends ActionFilter[GroupRtmRequest] {
  val MAX_MEDIA_ALLOWED = 10

  val maxMediaAllowed = if (config.hasPath("edc.service.rtm.max_concurrent_media")) config.getInt("edc.service.rtm.max_concurrent_media") else MAX_MEDIA_ALLOWED

  def filter[A](req: GroupRtmRequest[A]): Future[Option[Result]] = {
    Future.successful( if (req.toList.length > maxMediaAllowed) Some(Results.BadRequest(s"Max media allowed: ${maxMediaAllowed}")) else None)
  }
}
