addSbtCoursier

addSbtPlugin("com.typesafe.play" % "sbt-plugin"    % "2.8.8")
addSbtPlugin("org.scoverage"     % "sbt-scoverage" % "1.5.1")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"  % "1.5.1")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.2"
