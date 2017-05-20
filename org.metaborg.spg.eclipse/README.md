# SPG Eclipse

Eclipse integration for SPG.

## Requirements

- Eclipse Neon or higher

## Developing

This plugin depends on `org.metaborg.spg.core`. Because it is an Eclipse plugin, it uses Tycho for dependency management. Tycho is different from Maven in two aspects: first, it requires its dependencies to be OSGi packages, and second, it resolves these dependencies in the _Target platform_. To develop in Eclipse, you need to perform these steps:

1. To make `org.metaborg.spg.core` an OSGi bundle, run `sbt osgiBundle` in `org.metaborg.spg.core` and copy `META-INF/MANIFEST.MF` from the generated jar to `org.metaborg.spg.core/META-INF/MANIFEST.MF`. Congratulations, `org.metaborg.spg.core` is now an OSGi bundle.

2. Tycho resolves its dependencies (specified in `META-INF/MANIFEST.MF`) in the _Target platform_, which by default is the _Running platform_. Import `spg.core` in Eclipse to make it part of the Target platform.

3. Install the _Scala IDE for Eclipse_ plugin. Without this plugin, Eclipse shows errors that it cannot resolve types in `org.metaborg.spg.core`, even though it allows you to run `org.metaborg.spg.eclipse` without problems.

4. `org.metaborg.spg.core` depends on third-party code that is not included in the default Spoofax Eclipse installation and thus not available in the _Target platform_. Spoofax's way of working around this problem is by importing the `org.metaborg.spoofax.eclipse.externaldeps` project, which contains all third-party dependencies. I have added all `org.metaborg.spg.core`s dependencies to this project.

## Usage

After building the project, the update site will be located at
`org.metaborg.spg.eclipse.site/target/site`. Use this update site in Spoofax
Eclipse 2.1.0 to install the plugin.

