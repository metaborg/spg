# Spoofax Generator

[![Build Status](http://buildfarm.metaborg.org/buildStatus/icon?job=metaborg/spg/master)](http://buildfarm.metaborg.org/job/metaborg/job/spg/job/master/)

## Building

Pom-first and manifest-first projects cannot be mixed in the same reactor build ([source](https://goo.gl/akexsK)), so we need to separately build `org.metaborg.spg.sentence` and `org.metaborg.spg.sentence.eclipse*`:

```
mvn clean install -f org.metaborg.spg.sentence.shared/pom.xml
mvn clean install -f org.metaborg.spg.sentence/pom.xml
mvn clean install -f org.metaborg.spg.sentence.antlr/pom.xml
mvn clean install -f org.metaborg.spg.sentence.eclipse.externaldeps/pom.xml
mvn clean install -f pom.xml
```

## Testing

Only the `org.metaborg.spg.sentence` project contains tests. To run the tests:

```
mvn test -f org.metaborg.spg.sentence/pom.xml
mvn test -f org.metaborg.spg.sentence.antlr/pom.xml
```

## Usage

Make sure you are using the Java parse table generator. This is the case when `metaborg.yaml` contains the following:

```
language:
  sdf:
    sdf2table: java
```

### Using org.metaborg.spg.sentence.(sdf|antlr)

The `Main` class expects as its first and second argument the path to a Spoofax language artifact and the path to a Spoofax project, respectively.

### Using org.metaborg.spg.sentence.eclipse*

An update site is generated at `org.metaborg.spg.sentence.antlr.eclipse.site/target/site/`. This Eclipse plugin contains both the SDF-generator (ambiguity testing) and ANTLR-generator (difference testing). To install the plugin in Eclipse:

1. _Help_ > _Install New Software..._
2. _Work with_: the built update site.
3. Check the _Spoofax Generator Eclipse Plugin_
4. Follow the installation process.

After installing the Eclipse plugin:

1. Select the project in the _Package Explorer_.
2. Click _Spoofax (meta)_ > _SPG_ > _Generate sentences_.
3. Choose a maximum term size and maximum number of terms.
4. The generated sentences appear in the _Console_.

Warning: If you rebuild this project, uninstall the Eclipse plugin, and reinstall the Eclipse plugin, then Eclipse still uses the old (now uninstalled) version. The only solution I have found is to download a fresh Spoofax Eclipse from [spoofax.org](https://spoofax.org).

### Using org.metaborg.spg.sentence.evaluation

The `Main` class runs the generator on a predefined list of languages. These projects can be found [here](https://github.com/spg-subjects). Use `download.sh` to download these languages. To run the main class:

```
mvn clean package exec:java
```

