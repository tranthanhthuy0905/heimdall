package models.auth

import com.evidence.service.common.auth.jwt.JWTConstants
import com.nimbusds.jwt.{JWT, JWTClaimsSet}

import scala.collection.JavaConverters._

/**
 * General Axon wrapper over standard JWT abstraction.
 *
 * JWT Concepts:
 *  - Audience: the entity who is being manipulated (or who's sub-entities are
 *    being manipulated).
 *  - Subject: the entity performing the action.
 *
 * Since these are evidence.com entities, we must represent them according to
 *  the Ecom Data Model.  All entities can be represented using a 3-Tuple
 *  (guid, type, partitionKey).
 *
 *  @see http://static.javadoc.io/com.nimbusds/nimbus-jose-jwt/3.10/com/nimbusds/jwt/JWT.html
 *  @see https://jwt.io/introduction/
 */
class CommonJWTWrapper(val jwt: JWT) {
  protected val claimSet: JWTClaimsSet = jwt.getJWTClaimsSet

  /** analogous to the TID ID of the entity performing action */
  val subjectId: String = claimSet.getSubject
  /** analogous to the TID ID of the entity being manipulated */
  val audienceId: String = claimSet.getAudience.get(0)

  val claims: Set[String] = claimSet.getClaims.keySet().asScala.toSet

  val roles: Set[String] =
    if (claims.contains(JWTConstants.RolesClaim)) {
      claimSet.getStringListClaim(JWTConstants.RolesClaim).asScala.toSet
    } else { Set.empty[String] }
  val scopes: Set[String] =
    if (claims.contains(JWTConstants.ScopesClaim)) {
      claimSet.getStringListClaim(JWTConstants.ScopesClaim).asScala.toSet
    } else { Set.empty[String] }
}

/**
 * Specialized Axon JWT wrapper type which provides data fields analogous to the entity type and
 * domain data components of Axon TIDs whereas [[CommonJWTWrapper]] provides only the ID field.
 */
abstract class JWTWrapper(jwt: JWT) extends CommonJWTWrapper(jwt) {
  /** analagous to the TID entity type of the entity performing action */
  val subjectType: String
  /** analagous to the TID entity type of the entity being manipulated */
  val audienceType: String
  /** analagous to the TID domain of the entity performing action */
  val subjectDomain: Option[String]
  /** analagous to the TID domain of the entity being manipulated */
  val audienceDomain: Option[String]
}

object JWTWrapper {
  def apply(jwt: JWT): JWTWrapper = {
    val version = jwt.getJWTClaimsSet.getStringClaim(JWTConstants.VersionClaim)
    version match {
      case "2" => new JWTWrapperV2(jwt)
      case _ => throw UnsupportedClaimVersionException(version)
    }
  }
}

class JWTWrapperV2(jwt: JWT) extends JWTWrapper(jwt) {
  val subjectType = claimSet.getStringClaim(JWTConstants.SubjectTypeClaim)
  val audienceType = claimSet.getStringClaim(JWTConstants.AudienceTypeClaim)
  val subjectDomain = Option(claimSet.getStringClaim(JWTConstants.SubjectDomainClaim))
  val audienceDomain = Option(claimSet.getStringClaim(JWTConstants.AudienceDomainClaim))
}

/**
 * Thrown when a JWT is encountered that is of an unsupported version.
 *
 * At the time of writing JWTs of version 1 and 2 are supported.
 */
case class UnsupportedClaimVersionException(version: String)
  extends Exception(s"Version '$version' is not a recognized JWT version.", null)
