# SPG Command

A command-line interface to SPG.

## Usage

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

## Examples

The `examples/` directory contains Spoofax implementations for the following
languages in the form of git submodules: `L1`, `L2`, `L3`, `MiniJava`, and
`Tiger`.

1. Download the submodules (the `--remote` flag will fetch the latest
changes from upstream in each submodule):

  ```
  git submodule update --init --recursive --remote
  ```

2. Build some or all of the example languages using:

  ```
  (cd examples/scopes-frames/L1 && mvn clean verify)
  (cd examples/scopes-frames/L2 && mvn clean verify)
  (cd examples/scopes-frames/L3 && mvn clean verify)
  (cd examples/minijava && mvn clean verify)
  (cd examples/tiger/org.metaborg.lang.tiger/correct && mvn clean verify)
  ```

3. Run the generator. For example, in case of `Tiger`:

  ```
  sbt "run-main org.metaborg.spg.cmd.Main <sdf-path> <nabl-path> examples/tiger/org.metaborg.lang.tiger/correct"
  ```

Note: Bash uses an exclamation mark for [history
expansions](http://unix.stackexchange.com/a/33340/92581). If your SDF or NaBL
path contains an exclamation mark you may need to escape it or disable history
expansions.
