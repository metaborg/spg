lazy val commonSettings = Seq(
  organization := "org.metaborg",
  version := "2.1.0",
  scalaVersion := "2.11.8",

  // Allow overwriting non-SNAPSHOT build (http://stackoverflow.com/a/26089552/368220)
  isSnapshot := true,

  // Shade, because we depend on com.netflix.rxjava:rxjava-core (through org.metaborg:core) and io.reactivex:rxjava (through io.reactivex:rxscala)
  assemblyShadeRules in assembly := Seq(
    ShadeRule
      .rename("rx.**" -> "shadeio.@1")
      .inLibrary("io.reactivex" % "rxjava" % "1.2.2")
  )
)

// Jenkins looks for artifacts in a directory dependent on the build; change publishTo to reflect this.
publishTo in ThisBuild := {
  if (sys.env.contains("JENKINS_HOME"))
    Some(Resolver.file("file",  new File(s"${sys.env("JENKINS_HOME")}/m2repos/${sys.env("EXECUTOR_NUMBER")}")))
  else
    Some(Resolver.mavenLocal)
}

lazy val core = (project in file("org.metaborg.spg.core"))
  .settings(commonSettings)

lazy val cmd = (project in file("org.metaborg.spg.cmd"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("org.metaborg.spg.cmd.Main")
  )
