# SPG Command

A command-line interface to SPG.

## Usage

If you've followed the build instructions on the parent project there will be
a fat JAR located at `org.metaborg.spg.cmd/target/scala-2.11/`. You can invoke
this JAR using:

```
java -jar org.metaborg.spg.cmd/target/scala-2.11/org.metaborg.spg.cmd-assembly-2.2.1.jar
```

The CLI supports two modes of operation: generating random _sentences_, which
can be used to test for _grammar ambiguities_, and generating random _terms_,
which can be used to test for _type soundness_.

```
Usage

 spg [options] command [command options]

Options

   --limit=NUM      : Number of terms to generate (default: 1,000,000)
   --seed           : Seed for the random number generator (default: random)
   --size-limit=NUM : Maximum size of terms to generate (default: 60)

Commands

   sentence [command options] <sdfPath> <nablPath> <projectPath> : Generate random sentences
      --ambiguity : Test each generated sentence for ambiguity
      <sdfPath>   : Path to the SDF language implementation archive
      <nablPath>  : Path to the NaBL2 language implementation archive
      <projectPath> : Path to the Spoofax project of the language to generate terms for

   term [command options] <sdfPath> <nablPath> <projectPath> : Generate random terms
      --fuel=NUM : Fuel provided to the backtracker (default: 400)
      <sdfPath>   : Path to the SDF language implementation archive
      <nablPath>  : Path to the NaBL2 language implementation archive
      <projectPath> : Path to the Spoofax project of the language to generate terms for
```

Setting a seed makes the generator deterministic, which may help with
reproducing a bug.

