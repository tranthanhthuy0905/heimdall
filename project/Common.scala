import sbt._
import scala.collection.immutable.Seq

object Common {
  val serviceCommonVersion       = "27.3.3837"
  val serviceCommonCryptoVersion = "1.4.+"
  val paradiseVersion            = "2.1.0"
  val scalaTestVersion           = "3.0.5"
  val scalaMockVersion           = "4.1.0"
  val json4sTestVersion          = "3.5.3"
  val serviceThriftVersion       = "31.0.5872"
  val pdpProtoVersion            = "0.2.195"
  val sageProtoVersion           = "0.6.1951"
  val nettyVersion               = "4.1.79.Final"
  val jacksonVersion             = "2.13.3"
  val minidevVersion             = "2.4.8"
  val gsonVersion                = "2.9.1"
  val esapiVersion               = "2.5.0.0"
  val xmlsecVersion              = "3.0.0"
  val protobufJavaVersion        = "3.21.4"
  val commonCollectionVersion    = "3.2.2"
  val notYetCommonsSslVersion    = "0.3.15"
  val logbackCoreVersion         = "1.2.11"
  val apacheHttpVersion          = "4.5.13"
  val snakeYamlVersion           = "1.31"

  val resolvers = Seq("Nexus" at "https://nexus.taservs.net/content/groups/public")

  val dockerBaseImage = "axonhub.azurecr.io/axon/amazoncorretto:11"

  lazy val protoDependencies = Seq("io.grpc" % "grpc-netty-shaded" % "1.42.1")

  val exclusions = Seq(
    ExclusionRule("log4j", "log4j"),
    ExclusionRule("org.slf4j", "slf4j-jdk14"),
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
    ExclusionRule("com.evidence", "service-common-logging_2.11") // todo remove this when all services have upgraded to new logging
  )
}
