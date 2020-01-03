package actions

import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config
import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionFilter, Result, Results}

import scala.concurrent.{ExecutionContext, Future}

case class FeatureValidationActionBuilder @Inject()()(implicit val executionContext: ExecutionContext, config: Config) {

  def build(featureFlag: String) = {
    FeatureValidationAction(config.hasPath(featureFlag) && config.getBoolean(featureFlag))()(executionContext)
  }
}

case class FeatureValidationAction @Inject()(featureEnabled: Boolean)()(
  implicit val executionContext: ExecutionContext)
  extends ActionFilter[HeimdallRequest]
    with LazyLogging {

  def filter[A](request: HeimdallRequest[A]): Future[Option[Result]] = {
    featureEnabled match {
      case true =>
        Future(None)
      case false =>
        Future(Some(Results.NotFound))
    }
  }
}
