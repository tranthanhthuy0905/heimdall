package utils

import java.util.UUID
import java.util.UUID.randomUUID

import com.evidence.service.komrade.thrift.{Partner, User}
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import models.auth.JWTWrapper
import models.common.{MediaIdent, RtmQueryParams}

import scala.collection.immutable.Map

/**
  * TestHelper creates objects containing only required (absolutely necessary) attributes.
  */
object TestHelper {

  def getUser(username: String) = User(username = Some(username))

  def getPartner(domain: String) = Partner(domain = Some(domain))

  def getRtmQueryParams(partnerId: UUID, params: Map[String, String]): RtmQueryParams = {
    val testMediaIdent = MediaIdent(List(randomUUID), List(randomUUID), partnerId)
    RtmQueryParams(testMediaIdent, "/some/path", params)
  }

  def getJWTWrapper(partnerId: String, userId: String): JWTWrapper = {
    val claimsSet = new JWTClaimsSet.Builder().subject(userId).audience(partnerId).claim("ver", "2")
    val jwt= new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet.build())
    JWTWrapper(jwt)
  }

}
