name := "fragments-4"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository-new"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"

libraryDependencies += "org.metaborg" % "org.metaborg.core" % "2.0.0-SNAPSHOT"

libraryDependencies += "org.metaborg" % "org.metaborg.util" % "2.0.0-SNAPSHOT"

libraryDependencies += "org.metaborg" % "org.metaborg.spoofax.core" % "2.0.0-SNAPSHOT"
