name := "fragments-4"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += Resolver.mavenLocal

resolvers += "Metaborg Release Repository" at "http://artifacts.metaborg.org/content/repositories/releases/"

resolvers += "Metaborg Snapshot Repository" at "ttp://artifacts.metaborg.org/content/repositories/snapshots/"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.5"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"

libraryDependencies += "org.metaborg" % "org.metaborg.core" % "2.0.0-SNAPSHOT"

libraryDependencies += "org.metaborg" % "org.metaborg.util" % "2.0.0-SNAPSHOT"

libraryDependencies += "org.metaborg" % "org.metaborg.spoofax.core" % "2.0.0-SNAPSHOT"
