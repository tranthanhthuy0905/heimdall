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
  guice
)

libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)

libraryDependencies ++= Seq(
  "com.evidence" %% "service-common" % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-logging-macro" % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-auth" % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-finagle" % Common.serviceThriftVersion,
  "com.evidence" %% "service-common-zookeeper" % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-cache" % Common.serviceCommonVersion
)

// Thrift Services
libraryDependencies ++= Seq(
    "com.evidence" %% "service-common-finagle" % Common.serviceThriftVersion,
    "com.evidence" %% "dredd-service-thrift" % Common.serviceThriftVersion
)

// Exclusions
libraryDependencies ~= {
    _.map(_.excludeAll(Common.exclusions: _*))
}

updateOptions := updateOptions.value.withCachedResolution(true)

// Exclude development configs from zip package
mappings in Universal := {
    val origMappings = (mappings in Universal).value
    origMappings.filterNot { case (_, file) => file.endsWith("envs/dev.conf.tmp") || file.endsWith("envs/dev.conf") }
}

