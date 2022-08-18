import com.evidence.service.common.logging.LazyLogging
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.{RequestHeader, Result}
import play.api.mvc.Results.NotFound
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper}

import javax.inject._
import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject()(
                            env: Environment,
                            config: Configuration,
                            sourceMapper: OptionalSourceMapper,
                            router: Provider[Router]
                            ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with LazyLogging {

  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    logger.error("notFoundEndpoint")(
      "path" -> request.path,
      "method" -> request.method,
      "host" -> request.host
    )
    Future.successful(
      NotFound("The endpoint you requested does not exist!")
    )
  }

}
