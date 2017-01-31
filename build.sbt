name := "spg"

organization := "org.metaborg"

version := "2.1.0"

scalaVersion := "2.11.8"

resolvers += Resolver.mavenLocal

resolvers += "Metaborg Release Repository" at "http://artifacts.metaborg.org/content/repositories/releases/"

resolvers += "Metaborg Snapshot Repository" at "http://artifacts.metaborg.org/content/repositories/snapshots/"

// Logging
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
libraryDependencies += "ch.qos.logback" %  "logback-classic" % "1.1.7"

// Testing
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"

// Metaborg
libraryDependencies += "org.metaborg" % "org.metaborg.core" % "2.1.0"
libraryDependencies += "org.metaborg" % "org.metaborg.util" % "2.1.0"
libraryDependencies += "org.metaborg" % "org.metaborg.spoofax.core" % "2.1.0"

// Scala Guice magic
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.1.0"

// Observables
libraryDependencies += "io.reactivex" %% "rxscala" % "0.26.4"

// For parsing CLI options
libraryDependencies += "org.backuity.clist" %% "clist-core"   % "3.2.2"
libraryDependencies += "org.backuity.clist" %% "clist-macros" % "3.2.2" % "provided"
