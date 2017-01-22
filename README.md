# Generator

Generator is a language-parametric generator of well-formed terms. In takes a
language specification in the form of a Spoofax project and returns an
observable of well-formed terms.

## Building

### Publishing Locally

Use `sbt publish-local` to publish the artifact to your local Ivy repository.
Use `sbt publish-m2` to publish the artifact to your local Maven repository.

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

First, download the submodules:

```
git submodule update --init --recursive --remote
```

Second, build the language. For example, in case of `Tiger`:

```
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
