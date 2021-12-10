// *******************************
// Common Settings
// *******************************

ThisBuild / organization := "com.evidence"

ThisBuild / organizationHomepage := Some(url("https://www.evidence.com"))

// Nexus Repo Publishing
// *******************************
ThisBuild / credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

ThisBuild / publishTo := {
  val nexus = "https://nexus.taservs.net/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots/")
  else
    Some("releases" at nexus + "content/repositories/releases")
}
