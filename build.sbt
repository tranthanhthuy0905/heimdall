name := "heimdall"
scalaVersion := "2.12.6"

PlayKeys.playRunHooks += ConfigurationHook()
resolvers ++= Common.resolvers

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayAkkaHttpServer)

// SBT packager
enablePlugins(JavaAppPackaging)

// enable docker build
enablePlugins(DockerPlugin)
enablePlugins(UniversalPlugin)

Docker / packageName := packageName.value
Docker / version := version.value
dockerBaseImage := Common.dockerBaseImage
dockerAliases := Seq(
  dockerAlias.value.withUsername(Some("ecom"))
)
Docker / daemonUserUid := None
Docker / daemonUser := "daemon"
Docker / daemonGroupGid := None

// Do not run scaladoc/javadoc
Compile / doc / sources := Seq.empty
Compile / packageDoc / publishArtifact := false

routesGenerator := InjectedRoutesGenerator

libraryDependencies ++= Seq(
  guice,
  ws,
  caffeine
)

//https://github.com/playframework/play-ws/pull/573
// version 2.1.3 is last version depends on scala-java8-compat 0.9.1
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.1.3",
)

// avoid binary conflict dependencies
dependencyOverrides ++= Seq(
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1",
  "com.fasterxml.jackson.core" % "jackson-core" % Common.jacksonVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % Common.jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % Common.jacksonVersion,
  "com.google.code.gson" % "gson" % Common.gsonVersion,
  "org.owasp.esapi" % "esapi" % Common.esapiVersion,
  "org.apache.santuario" % "xmlsec" % Common.xmlsecVersion,
  "com.google.protobuf" % "protobuf-java" % Common.protobufJavaVersion,
  "commons-collections" % "commons-collections" % Common.commonCollectionVersion,
  "ca.juliusdavies" % "not-yet-commons-ssl " % Common.notYetCommonsSslVersion,
  "ch.qos.logback" % "logback-core" % Common.logbackCoreVersion,
  "org.apache.httpcomponents" % "httpclient" % Common.apacheHttpVersion,
  "net.minidev" % "json-smart" % Common.minidevVersion,
  "io.netty" % "netty-codec" % Common.nettyVersion,
  "io.netty" % "netty-codec-http" % Common.nettyVersion,
  "io.netty" % "netty-codec-http2" % Common.nettyVersion,
  "org.yaml" % "snakeyaml" % Common.snakeYamlVersion,
)

libraryDependencies ++= Seq(
  "com.evidence" %% "service-common"               % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-logging-macro" % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-auth"          % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-finagle"       % Common.serviceThriftVersion,
  "com.evidence" %% "service-common-zookeeper"     % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-cache"         % Common.serviceCommonVersion,
  "com.evidence" % "service-common-crypto"         % Common.serviceCommonCryptoVersion,
  "com.evidence" %% "service-common-monad"         % Common.serviceCommonVersion,
  "com.evidence" %% "service-common-queue"         % Common.serviceCommonVersion
)

// Thrift Services
libraryDependencies ++= Seq(
  "com.evidence" %% "audit-service-thrift"    % Common.serviceThriftVersion,
  "com.evidence" %% "dredd-service-thrift"    % Common.serviceThriftVersion,
  "com.evidence" %% "komrade-service-thrift"  % Common.serviceThriftVersion,
  "com.evidence" %% "sessions-service-thrift" % Common.serviceThriftVersion,
  "com.evidence" % "edc-thrift-java"          % Common.serviceThriftVersion
)

//GRPC Services
libraryDependencies ++= Common.protoDependencies
libraryDependencies ++= Seq(
  "com.evidence" %% "pdp-proto"  % Common.pdpProtoVersion,
  "com.evidence" %% "sage-proto" % Common.sageProtoVersion,
)

// Test
libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play"       % "3.1.2"  % Test,
  "com.github.sebruck"     %% "scalatest-embedded-redis" % "0.4.0"  % Test,
  "org.mockito"            % "mockito-core"              % "2.21.0" % Test
)

// Exclusions
libraryDependencies ~= {
  _.map(_.excludeAll(Common.exclusions: _*))
}

updateOptions := updateOptions.value.withCachedResolution(true)
Test / javaOptions += "-Dconfig.file=conf/env/test.conf"

// Exclude development configs from zip package
Universal / mappings := {
  val origMappings = (Universal / mappings).value
  origMappings.filterNot { case (_, file) => file.endsWith("env/localdev.conf") }
}

coverageMinimum := 70
coverageFailOnMinimum := false
coverageHighlighting := true
