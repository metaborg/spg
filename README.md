# SPG

SPG (SPoofax Generator) is a language-parametric generator of random
well-formed terms. It takes a language specification in the form of a Spoofax
project and returns an observable of well-formed terms.

## Projects

This repository contains the following sub-projects:

- `org.metaborg.spg.core`: The Spoofax generator.
- `org.metaborg.spg.cli`: A command-line interface to the generator.
- `org.metaborg.spg.eclipse`: An Eclipse plugin for the generator.
- `org.metaborg.spg.eclipse.externaldeps`: External dependencies for the Eclipse plugin.
- `org.metaborg.spg.eclipse.feature`: Expose the Eclipse plugin as feature.
- `org.metaborg.spg.eclipse.site`: Generate an update site for the Eclipse plugin as feature.

## Building

First, build `org.metaborg.spg.{cmd,core}` using:

```
sbt clean assembly publish-m2
```

Second, build `org.metaborg.spg.eclipse.externaldeps` using:

```
mvn -f org.metaborg.spg.eclipse.externaldeps/pom.xml clean install
```

Third, build `org.metaborg.spg.eclipse*` using:

```
mvn -f org.metaborg.spg.eclipse/pom.xml clean install
mvn -f org.metaborg.spg.eclipse.feature/pom.xml clean install
mvn -f org.metaborg.spg.eclipse.site/pom.xml clean install
```

Alternatively, build `org.metaborg.spg.eclipse*` at once using:

```
mvn -f pom.xml clean install
```

## Usage

To use the CLI, see `org.metaborg.spg.cmd`.
To use the API, see `org.metaborg.spg.core`.
To use the Eclipse plugin, see `org.metaborg.spg.eclipse`.

