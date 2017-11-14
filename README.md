# Spoofax Generator

## Building

Pom-first and manifest-first projects cannot be mixed in the same reactor build ([source](https://goo.gl/akexsK)), so we need to separately build `org.metaborg.spg.sentence` and `org.metaborg.spg.sentence.eclipse*`:

```
mvn clean install -f org.metaborg.spg.sentence/pom.xml
mvn clean install -f pom.xml
```

## Developing

Import all projects in Spoofax Eclipse. Until the modified `org.metaborg.sdf2table` lands in master, also import that project.

## Usage

The generator is used differently dependong in the subproject. In any case, make sure you're using the Java parse table generator by adding to `metaborg.yaml`:

```
language:
  sdf:
    sdf2table: java
```

### org.metaborg.spg.sentence

The `Main` class expects as its first and second argument the path to a Spoofax language artifact and the path to a Spoofax project, respectively.

### org.metaborg.spg.sentence.eclipse*

With the Eclipse plugin:

1. Select the project in the _Package Explorer_.
2. Click _Spoofax (meta)_ > _SPG_ > _Generate sentences_.
3. Choose a maximum term size and maximum number of terms.
4. The generated sentences appear in the _Console_.

### org.metaborg.spg.sentence.evaluation

The `Main` class runs the generator on a predefined list of languages.

