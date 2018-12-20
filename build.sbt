name := "heimdall"
scalaVersion := "2.12.6"

PlayKeys.playRunHooks += ConfigurationHook()
resolvers ++= Common.resolvers

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayAkkaHttpServer)

javaOptions in Universal += "-J-Xmx2048M"

// Do not run scaladoc/javadoc
sources in(Compile, doc) := Seq.empty
publishArtifact in(Compile, packageDoc) := false

routesGenerator := InjectedRoutesGenerator

libraryDependencies ++= Seq(
  guice,
  ws
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ws" % "2.6.17"
)

libraryDependencies ++= Seq(
  "com.evidence" %% "service-common" % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-logging-macro" % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-auth" % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-finagle" % Common.serviceThriftVersion,
  "com.evidence" %% "service-common-zookeeper" % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-cache" % Common.serviceCommonVersion,
  "com.evidence" %  "service-common-crypto" % Common.serviceCommonCryptoVersion
)

// Thrift Services
libraryDependencies ++= Seq(
  "com.evidence" %% "audit-service-thrift" % Common.serviceThriftVersion,
  "com.evidence" %% "dredd-service-thrift" % Common.serviceThriftVersion,
  "com.evidence" %% "sessions-service-thrift" % Common.serviceThriftVersion,
  "com.evidence" % "edc-thrift-java" % Common.serviceThriftVersion
)

// Test
libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "org.mockito" % "mockito-core" % "2.10.0" % Test
)

// Exclusions
libraryDependencies ~= {
    _.map(_.excludeAll(Common.exclusions: _*))
}

updateOptions := updateOptions.value.withCachedResolution(true)

javaOptions in Test += "-Dconfig.file=conf/env/test.conf"

// Exclude development configs from zip package
mappings in Universal := {
    val origMappings = (mappings in Universal).value
    origMappings.filterNot { case (_, file) => file.endsWith("env/localdev.conf") }
}

coverageMinimum := 70
coverageFailOnMinimum := false
coverageHighlighting := true

