# spg.eclipse

Eclipse integration for SPG.

## Developing

This plugin depends on `spg.core`. Because it is an Eclipse plugin, it uses Tycho for dependency management. Tycho is different from Maven in two aspects: first, it requires its dependencies to be OSGi packages, and second, it resolves these dependencies in the _Target platform_. To develop in Eclipse, you need to perform these steps:

1. To make `spg.core` an OSGi bundle, run `sbt osgiBundle` in `spg.core` and copy `META-INF/MANIFEST.MF` from the generated jar to `spg.core/META-INF/MANIFEST.MF`. Congratulations, `spg.core` is now an OSGi bundle.

2. Tycho resolves its dependencies (specified in `META-INF/MANIFEST.MF`) in the _Target platform_, which by default is the _Running platform_. Import `spg.core` in Eclipse to make it part of the Target platform. You may need to install the _Scala IDE for Eclipse_ plugin as well.

3. `spg.core` depends on third-party code that is not included in the default Spoofax Eclipse installation and thus not available in the _Target platform_. Spoofax's way of working around this problem is by importing the `org.metaborg.spoofax.eclipse.externaldeps` project, which contains all third-party dependencies. I have added all `spg.core`s dependencies to this project.
