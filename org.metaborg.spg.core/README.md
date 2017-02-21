# SPG Core

The Spoofax generator.

## Building

The project uses sbt as build tool. Use `sbt publish-local` to publish the
artifact to your local Ivy repository and `sbt publish-m2` to publish o your
local Maven repository.

Note: The `org.metaborg.spg.eclipse` project relies on this project, and Tycho resolves POM
dependencies, but only if they are OSGi bundles. The `sbt-osgi` plugin adds
the `osgiBundle` task which creates an OSGi bundle for the project and changes
the `publish` task to publish an OSGi bundle instead of a raw JAR archive. It
is the `maven-bundle-plugin` for sbt.

Note: When developing an Eclipse plugin, Eclipse resolves its dependencies
(defined in `MANIFEST.MF`) in the Target platform. Use `sbt eclipse` to
transform this project into an Eclipse project.

## Usage

`Generator.generate` returns an `Observable[String]` that emits well-formed
terms.
