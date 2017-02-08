name := "org.metaborg.spg.core"

organization := "org.metaborg"

version := "2.1.0"

scalaVersion := "2.11.8"

// Use Maven-style naming instead of Ivy-style naming (http://stackoverflow.com/a/23582766/368220)
moduleName := name.value

// Disable using the Scala version in output paths and artifacts
crossPaths := false

// Allow overwriting non-SNAPSHOT build (http://stackoverflow.com/a/26089552/368220)
isSnapshot := true

// Publish as package OSGi settings
enablePlugins(SbtOsgi)

osgiSettings

OsgiKeys.exportPackage := Seq("org.metaborg.spg.core")

OsgiKeys.importPackage := Seq(
  "!sun.misc",
  "!sun.nio.cs",
  "!org.junit",
  "!javax*",
  "!org.xml*",
  "org.spoofax*",
  "org.metaborg*",
  "org.strategoxt.*",
  "org.apache.tools.ant",
  "*;provider=metaborg"
)

OsgiKeys.bundleSymbolicName := "org.metaborg.spg.core"

// Add resolvers
resolvers += Resolver.mavenLocal

resolvers += "Metaborg Release Repository" at "http://artifacts.metaborg.org/content/repositories/releases/"

resolvers += "Metaborg Snapshot Repository" at "http://artifacts.metaborg.org/content/repositories/snapshots/"

// Scala wrapper for slf4j
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"

// Scala testing framework
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"

// Metaborg (Spoofax)
libraryDependencies += "org.metaborg" % "org.metaborg.core" % "2.1.0"
libraryDependencies += "org.metaborg" % "org.metaborg.util" % "2.1.0"
libraryDependencies += "org.metaborg" % "org.metaborg.spoofax.core" % "2.1.0"

// Observables
libraryDependencies += "io.reactivex" %% "rxscala" % "0.26.4"

// OSGi
libraryDependencies += "org.osgi" % "org.osgi.core" % "4.3.0" % "provided"

// JSR305 Annotations (see http://stackoverflow.com/a/13162672/368220)
libraryDependencies += "com.google.code.findbugs" % "jsr305" % "1.3.+" % "compile"
