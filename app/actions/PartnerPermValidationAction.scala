package actions

import com.evidence.service.common.auth.{AuthErrors, CachingJOSEComponentFactory, KeyManager}
import com.evidence.service.common.auth.jwt.{JWTWrapper, VerifyingJWTParser}
import com.evidence.service.common.logging.LazyLogging
import com.typesafe.config.Config

import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionFilter, Result, Results}

import scala.concurrent.{ExecutionContext, Future}

case class PartnerPermValidationActionBuilder @Inject()(config: Config)(
  implicit val executionContext: ExecutionContext) {

  def build(partnerId: String) = {
    PartnerPermValidationAction(partnerId)(config)(executionContext)
  }
}

case class PartnerPermValidationAction @Inject()(partnerId: String)(config: Config)(
  implicit val executionContext: ExecutionContext
) extends ActionFilter[HeimdallRequest]
  with LazyLogging {

  protected val keyManager: KeyManager                        = KeyManager.apply(config)
  protected val componentFactory: CachingJOSEComponentFactory = new CachingJOSEComponentFactory(keyManager)
  protected val parser                                        = new VerifyingJWTParser(componentFactory)

  def filter[A](request: HeimdallRequest[A]): Future[Option[Result]] = {
    authorize(request.jwt, partnerId).map {
      case true =>
        None
      case _ =>
        logger.error("failedToEnforcePermissions")(
          "path"                  -> request.path,
          "query"                 -> request.queryString,
          "subjectId"                -> request.subjectId,
          "partnerId"             -> partnerId
        )
        Some(Results.Forbidden)
    }
  }

  private def authorize(jwt: String, partnerId: String): Future[Boolean] = {
    val result = parseJWT(jwt).map(jwtWrapper => jwtWrapper.subjectDomain.map(isAudience(_, partnerId)).getOrElse(false))
      .fold(_ => false, t => t)
    Future.successful(result)
  }

  private def parseJWT(jwtString: String): Either[AuthErrors, JWTWrapper] = {
    parser.parse(jwtString).map(JWTWrapper(_))
  }

  private def isAudience(audienceId: String, partnerId: String): Boolean = {
    audienceId.replace("-","").equalsIgnoreCase(partnerId.replace("_",""))
  }
}
