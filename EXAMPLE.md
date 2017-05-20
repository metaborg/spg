# Example

A small example to get started with SPG. First, build SPG and assemble a fat JAR:

```
sbt clean assemble
```

Then, clone a language project. For this example, we'll use `metaborg-scopes-frames`:

```
git clone -b generation git@github.com:MartijnDwars/metaborg-scopes-frames.git
```

If we invoke the fat JAR without arguments we'll get to see all available options (see also org.metaborg.spg.cmd project):

```
java -jar org.metaborg.spg.cmd/target/scala-2.11/org.metaborg.spg.cmd-assembly-2.1.0.jar
```

For example, to generate 100 semantically valid L0 terms:

```
java -jar org.metaborg.spg.cmd/target/scala-2.11/org.metaborg.spg.cmd-assembly-2.1.0.jar --limit=100 --size-limit=1000 --fuel=1000 --seed=0 term zip:/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0.spoofax-language!/ zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0.spoofax-language!/ /Users/martijn/Projects/metaborg-scopes-frames/L0
```

The output of above command is supposed to look like [this](https://gist.github.com/MartijnDwars/13f3588620820ce2292eba7fb8a2cec8). Hurray, we just generated 100 random terms.

