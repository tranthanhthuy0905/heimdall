import sbt._

object Common {
  val serviceCommonVersion = "19.13.1942"
  val serviceCommonCryptoVersion = "1.4.+"
  val paradiseVersion = "2.1.0"
  val scalaTestVersion = "3.0.5"
  val scalaMockVersion = "4.1.0"
  val json4sTestVersion = "3.5.3"
  val serviceThriftVersion = "23.82.2246"
  val pdpProtoVersion = "0.2.195"

  val resolvers = Seq(
    "Nexus" at "https://nexus.taservs.net/content/groups/public",
    "Twitter" at "https://maven.twttr.com",
    "OpenSaml" at "https://build.shibboleth.net/nexus/content/repositories/releases"
  )

  lazy val protoDependencies = Seq("io.grpc" % "grpc-netty-shaded" % "1.24.0")

  val exclusions = Seq(
    ExclusionRule("org.slf4j", "slf4j-jdk14"),
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
    ExclusionRule("com.evidence",  "service-common-logging_2.11") // todo remove this when all services have upgraded to new logging
  )
}
