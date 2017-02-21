lazy val core = project in file("org.metaborg.spg.core")

lazy val cmd = (project in file("org.metaborg.spg.cmd"))
  .dependsOn(core)

lazy val commonSettings = Seq(
  organization := "org.metaborg",
  version := "2.1.0",
  scalaVersion := "1.11.8"
)
