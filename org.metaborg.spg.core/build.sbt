name := "org.metaborg.spg.core"

organization := "org.metaborg"

version := "2.2.1"

scalaVersion := "2.11.8"

// When publishing to Maven, do not replace dots by hyphens (http://stackoverflow.com/a/23582766/368220).
moduleName := name.value

// Disable using the Scala version in output paths and artifacts
crossPaths := false

// Allow overwriting non-SNAPSHOT build (http://stackoverflow.com/a/26089552/368220)
isSnapshot := true

// Add MANIFEST.MF to the main binary jar (https://goo.gl/f5JhG6)
packageOptions in (Compile, packageBin) +=  {
  val file = baseDirectory.value / "META-INF" / "MANIFEST.MF"
  val manifest = Using.fileInputStream(file)( in => new java.util.jar.Manifest(in) )
  Package.JarManifest( manifest )
}

// Add resolvers
resolvers += Resolver.mavenLocal

resolvers += "Metaborg Release Repository" at "http://artifacts.metaborg.org/content/repositories/releases/"

resolvers += "Metaborg Snapshot Repository" at "http://artifacts.metaborg.org/content/repositories/snapshots/"

// Scala wrapper for slf4j
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"

// Scala testing framework
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"

// Metaborg (Spoofax)
libraryDependencies += "org.metaborg" % "org.metaborg.core" % "2.2.1"
libraryDependencies += "org.metaborg" % "org.metaborg.util" % "2.2.1"
libraryDependencies += "org.metaborg" % "org.metaborg.spoofax.core" % "2.2.1"

// Observables
libraryDependencies += "io.reactivex" %% "rxscala" % "0.26.4"

// JSR305 Annotations (see http://stackoverflow.com/a/13162672/368220)
libraryDependencies += "com.google.code.findbugs" % "jsr305" % "1.3.+" % "compile"
