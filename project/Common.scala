import sbt._

object Common {
  val serviceCommonVersion       = "26.6.3296"
  val serviceCommonCryptoVersion = "1.4.+"
  val paradiseVersion            = "2.1.0"
  val scalaTestVersion           = "3.0.5"
  val scalaMockVersion           = "4.1.0"
  val json4sTestVersion          = "3.5.3"
  val serviceThriftVersion       = "25.1.4638"
  val pdpProtoVersion            = "0.2.195"
  val sageProtoVersion           = "0.6.1951"

  val resolvers = Seq("Nexus" at "https://nexus.taservs.net/content/groups/public")

  val dockerBaseImage = "axonhub.azurecr.io/axon/amazoncorretto:11"

  lazy val protoDependencies = Seq("io.grpc" % "grpc-netty-shaded" % "1.42.1")

  val exclusions = Seq(
    ExclusionRule("org.slf4j", "slf4j-jdk14"),
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
    ExclusionRule("com.evidence", "service-common-logging_2.11") // todo remove this when all services have upgraded to new logging
  )
}
