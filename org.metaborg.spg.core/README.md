# SPG Core

The Spoofax generator.

## Requirements

- Spoofax 2.1.0

## Building

Use [sbt](http://scala-sbt.org) to build the project. Use `sbt publish-local`
to publish the artifact to your local Ivy repository and `sbt publish-m2` to
publish the artifact to your local Maven repository.

> Note: `org.metaborg.spg.eclipse` has a Tycho dependency on
`org.metaborg.spg.core`, but Tycho only resolves POM dependencies if they are
OSGi bundles. SBT has an `sbt-osgi` plugin that automatically turns a project
into an OSGi bundle, but `sbt-osgi` sets wrong version constraints. Instead, we
created a static `META-INF/MANIFEST.MF` that is bundled during packaging. If
you add dependencies to the project, don't forget to add them to the MANIFEST
as well.

> Note: When developing an Eclipse plugin, Eclipse resolves its dependencies
(defined in `MANIFEST.MF`) in the Target platform. By importing this project
in Eclipse, it automatically becomes part of the Target platform. Use
`sbt eclipse` to transform this project into an Eclipse project that can be
imported.

## Usage

`Generator.generate` returns an `Observable[String]` that emits well-formed
terms.

