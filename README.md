# Fragments

Fragments is a language-parametric test data generator. In goes a Spoofax project, out comes a stream of well-formed
programs. The input project is expected to contain a language specification in SDF3 (Syntax Definition Formalism) and
NaBL2 (Name Binding Language).

## Building

The generator depends on NaBL2 at version 2.1.0. In particular, the code is not compatible with 2.2.0-SNAPSHOT. The
path to NaBL2 is hardcoded in the source.

## Usage

`Generator.generate` reutrns an `Observable[String]` of well-formed terms.
How you use these terms depends on your use case. For eample:

```
$ sbt "run-main nl.tudelft.fragments.RichGenerator /Users/martijn/Projects/metaborg-tiger/org.metaborg.lang.tiger"
```

will run the generator for Tiger. The `RichGenerator` outputs running averages
on the generated terms.

### Publishing Locally

Use `sbt publish-local` to publish the artifact to the local Ivy repository. Use `sbt publish-m2`
to publish the artifact to the local Maven repository.

### Statistics

The `Statistics` observer slurps well-formed terms and outputs running averages
such as average use of each constructor.
