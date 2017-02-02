# SPG

SPG (SPoofax Generator) is a language-parametric generator of well-formed
terms. It takes a language specification in the form of a Spoofax project and
returns an observable of well-formed terms.

## Building

### Publishing Locally

Use `sbt publish-local` to publish the artifact to your local Ivy repository.
Use `sbt publish-m2` to publish the artifact to your local Maven repository.

The `spg.eclipse` project relies on this project, and Tycho resolves POM
dependencies, but only if they are OSGi bundles. The `sbt-osgi` plugin adds
the `osgiBundle` task which creates an OSGi bundle for the project and changes
the `publish` task to publish an OSGi bundle instead of a raw JAR archive. Like
`maven-bundle-plugin`, `sbt-osgi` is a wrapper for the `Bnd` tool.

When developing an Eclipse plugin, Eclipse resolves its dependencies (MANIFEST.MF)
in the Target platform. To get the osgi-ified `spg.eclipse` in Eclipse, first
create an Eclipse project (`sbt eclipse`) and then import it in Eclipse. 

## Usage

The generator can be invoked either through its CLI or API.

### Command Line Interface

```
Usage

 generator [options] <sdfPath> <nablPath> <projectPath> : Generate random well-formed terms

Options

   --fuel=NUM              : Fuel provided to the backtracker (default: 400)
   --limit=NUM             : Number of terms to generate (default: -1)
   --semantics-path=STRING : Path to the static semantics specification (default: trans/static-semantics.nabl2)
   --size-limit=NUM        : Maximum size of terms to generate (default: 60)
   --verbosity=STRING      : Verbosity of the output as log level (default: ERROR)

Arguments

   <sdfPath>     : Path to the SDF language implementation archive
   <nablPath>    : Path to the NaBL2 language implementation archive
   <projectPath> : Path to the Spoofax project of the language to generate terms for
```

### Application Programming Interface

`Generator.generate` returns an `Observable[String]` that emits well-formed terms.

## Examples

The `examples/` directory contains Spoofax implementations for the following
languages in the form of git submodules: `L1`, `L2`, `L3`, `MiniJava`, and
`Tiger`.

First, download the submodules (the `--remote` flag will fetch the latest
changes from upstream in each submodule):

```
git submodule update --init --recursive --remote
```

Second, build some or all of the example languages using:

```
(cd examples/scopes-frames/L1 && mvn clean verify)
(cd examples/scopes-frames/L2 && mvn clean verify)
(cd examples/scopes-frames/L3 && mvn clean verify)
(cd examples/minijava && mvn clean verify)
(cd examples/tiger/org.metaborg.lang.tiger/correct && mvn clean verify)
```
Third, run the generator. For example, in case of `Tiger`:

```
sbt "run-main nl.tudelft.fragments.GeneratorEntryPoint <sdf-path> <nabl-path> examples/tiger/org.metaborg.lang.tiger/correct"
```

Note: Bash uses an exclamation mark for [history
expansions](http://unix.stackexchange.com/a/33340/92581). If your SDF or NaBL
path contains an exclamation mark you may need to escape it or disable history
expansions.
