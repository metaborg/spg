# Spoofax Generator

## Building

First, build `org.metaborg.spg.sentence`:

```
mvn clean install -f org.metaborg.spg.sentence/pom.xml
```

Then, build `org.metaborg.spg.sentence.eclipse*`:

```
mvn clean install -f pom.xml
```

## Developing

Import all projects in Spoofax Eclipse. Until the modified `org.metaborg.sdf2table` lands in master, also import that project.

## Usage

First, make sure your project uses the non-default Java implementation of sdf2table by adding to `metaborg.yaml`:

```
language:
  sdf:
    sdf2table: java
```

