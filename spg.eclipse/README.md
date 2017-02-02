# spg.eclipse

Eclipse integration for SPG.

## Developing

This plugin depends on `spg.core`. Because it is an Eclipse plugin, it uses Tycho for dependency management. Tycho requires its dependencies to be OSGi packages and resolves these dependencies in the _Target platform_. This requires two steps:

1. To make `spg.core` an OSGi bundle, run `sbt osgiBundle` and copy `META-INF/MANIFEST.MF` from the generated jar to the root of the project. Congratulations, `spg.core` is now an OSGi bundle.

2. Tycho resolves its dependencies (specified in `META-INF/MANIFEST.MF`) in the _Target platform_, which by default is the _Running platform_. Import `spg.core` in Eclipse to make it part of the Target platform. You may need to install the Eclipse Scala plugin as well.

Furthermore, `spg.core` depends on some third-party code that is not included in the default Spoofax Eclipse installation. The `org.metaborg.spoofax.eclipse.externaldeps` project creates a single OSGi bundle that exposes third-party code. I have modified this project to contain `spg.core`s dependencies. You will need to import this project as well.
