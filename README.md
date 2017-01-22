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
