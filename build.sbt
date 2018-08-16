import play.sbt.routes.RoutesKeys

name := "heimdall"

scalaVersion := "2.12.6"

resolvers ++= Common.resolvers

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayAkkaHttpServer)

javaOptions in Universal += "-J-Xmx2048M"

// Do not run scaladoc/javadoc
sources in(Compile, doc) := Seq.empty
publishArtifact in(Compile, packageDoc) := false

// Setup for dev envs
PlayKeys.devSettings += "HEIMDALL_ENV" -> "dev"

libraryDependencies ++= Seq(
    guice,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
    "org.mockito" % "mockito-core" % "2.18.3" % Test,
    "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1"
)

libraryDependencies ++= Seq(
    "com.evidence" %% "service-common" % Common.serviceCommonVersion,
    "com.evidence" %% "service-common-auth" % Common.serviceCommonVersion,
    "com.evidence" %% "service-common-logging-macro" % Common.serviceCommonVersion,
    "com.evidence" %% "service-common-zookeeper" % Common.serviceCommonVersion,
    "com.evidence" %% "service-common-cache" % Common.serviceCommonVersion
)

// For date manipulation
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.20.0"

// Thrift Services
libraryDependencies ++= Seq(
    "com.evidence" %% "service-common-finagle" % Common.serviceThriftVersion,
    "com.evidence" %% "sessions-service-thrift" % Common.serviceThriftVersion,
    "com.evidence" %% "komrade-service-thrift" % Common.serviceThriftVersion
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

