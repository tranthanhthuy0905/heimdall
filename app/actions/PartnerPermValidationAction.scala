package actions

import com.evidence.service.common.auth.{CachingJOSEComponentFactory, KeyManager}
import com.evidence.service.common.auth.jwt.{JWTWrapper, VerifyingJWTParser}
import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.thrift.v2.ServiceErrorCode.Badrequest
import com.evidence.service.thrift.v2.ServiceException
import com.nimbusds.jwt.JWT
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
    val jwtWrapper = parseJwt(jwt)
    Future.successful(isAudience(jwtWrapper, partnerId))
  }

  private def parseJwt(jwtStr: String): JWTWrapper =
    parser.parse(jwtStr) match {
      case Left(err) =>
        logger.info("auth_error")("error" -> err)
        throw ServiceException(Badrequest, Some(s"Invalid JWT: $err"))
      case Right(jwt: JWT) =>
        val wrapper = JWTWrapper(jwt)
        wrapper.subjectDomain match {
          case None         => throw ServiceException(Badrequest, Some("User domain is required"))
          case Some(domain) => wrapper
        }
    }

  private def isAudience(jwtWrapper: JWTWrapper, partnerId: String): Boolean =
    partnerId.replace("-", "").equalsIgnoreCase(jwtWrapper.audienceId.replace("-", ""))
}
