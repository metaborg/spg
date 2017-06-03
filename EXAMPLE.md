# Example

A small example to get started with SPG. First, build SPG and assemble a fat JAR:

```
sbt clean assemble
```

Then, clone a language project. For this example we'll use the `metaborg-scopes-frames` project that contains L0-L3, four languages of increasing complexity. Clone and checkout `generation`:

```
git clone -b generation git@github.com:MartijnDwars/metaborg-scopes-frames.git ~/Projects/metaborg-scopes-frames
```

Build the language projects:

```
mvn -f ~/Projects/metaborg-scopes-frames/pom.xml clean verify
```

By invoking the fat JAR without arguments you'll get to see all available options:

```
java -jar org.metaborg.spg.cmd/target/scala-2.11/org.metaborg.spg.cmd-assembly-2.2.1.jar
```

## Sentence generation

Sentence generation can be useful when testing a grammar for ambiguities. To generate 100 sentences in L0:

```
java -jar org.metaborg.spg.cmd/target/scala-2.11/org.metaborg.spg.cmd-assembly-2.2.1.jar --limit=100 --size-limit=1000 --seed=0 sentence zip:/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.2.1.spoofax-language!/ zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.2.1.spoofax-language!/ /Users/martijn/Projects/metaborg-scopes-frames/L0
```

By default, the generated sentences are not used to test for ambiguity. By providing the `--ambiguity` flag, the generated sentences are parsed until one is ambiguous. The ambiguous sentence is shrunken to find a minimal example of ambiguity. Since L0-L3 are not ambiguous, you'll need to test this on a language that is actually ambiguous (hint: many grammars in MetaBorgCube are ambiguous).

## Term generation

Term generation can be useful for testing type soundness or for testing compiler optimizations. For example, to generate 100 terms in L0:

```
java -jar org.metaborg.spg.cmd/target/scala-2.11/org.metaborg.spg.cmd-assembly-2.2.1.jar --limit=100 --size-limit=1000 --fuel=1000 --seed=0 term zip:/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.2.1.spoofax-language!/ zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.2.1.spoofax-language!/ /Users/martijn/Projects/metaborg-scopes-frames/L0
```

The output of above command is supposed to look like [this](https://gist.github.com/MartijnDwars/13f3588620820ce2292eba7fb8a2cec8). Hurray, we just generated 100 random terms! Now do the same for L1, L2, and L3. You'll notice that the performance of the generator degrades and the terms become less "interesting" as the language becomes more complex. At some point it's safe to assume that random testing is no longer feasible.

The generator is bad at handling certain language constructs, particularly those that involve name resolution. For example, try commenting out the constraint generation rule for QVar in L3 (`L3/trans/static-semantics.nabl2` lines 109-116) and run the generator on L3 again. By removing the rule for QVar the generator will no longer consider this as possible expression. You'll notice that the generated terms look quite different (larger and more "interesting").

