name := "fragments-4"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += Resolver.mavenLocal

resolvers += "Metaborg Release Repository" at "http://artifacts.metaborg.org/content/repositories/releases/"

resolvers += "Metaborg Snapshot Repository" at "http://artifacts.metaborg.org/content/repositories/snapshots/"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"

libraryDependencies += "ch.qos.logback" %  "logback-classic" % "1.1.7"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"

libraryDependencies += "org.metaborg" % "org.metaborg.core" % "2.1.0-SNAPSHOT"

libraryDependencies += "org.metaborg" % "org.metaborg.util" % "2.1.0-SNAPSHOT"

libraryDependencies += "org.metaborg" % "org.metaborg.spoofax.core" % "2.1.0-SNAPSHOT"

libraryDependencies += "net.codingwell" %% "scala-guice" % "4.1.0"
