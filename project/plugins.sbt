addSbtCoursier

// Uncomment to check dependencies graph
// https://github.com/sbt/sbt-dependency-graph#main-tasks
//addDependencyTreePlugin

addSbtPlugin("com.typesafe.play" % "sbt-plugin"    % "2.8.11")
addSbtPlugin("org.scoverage"     % "sbt-scoverage" % "1.5.1")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"  % "1.5.1")

// Docker
resolvers +=  "Nexus" at "https://nexus.taservs.net/content/groups/public"
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.7")
addSbtPlugin("com.axon.sbt" % "sbt-axon-docker" % "0.5.0")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.2"
