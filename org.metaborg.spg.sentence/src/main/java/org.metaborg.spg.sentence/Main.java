package org.metaborg.spg.sentence;

import java.util.Optional;

import org.spoofax.interpreter.terms.IStrategoTerm;

public class Main {
  public static void main(String[] args) throws Exception {
    // TODO: Use Spoofax Core to get the project. In the Eclipse integration, the project can be passed to the generator directly.

    SentenceGenerator sentenceGenerator = new SentenceGenerator(null);

    for (int i = 0; i < 1000; i++) {
      Optional<IStrategoTerm> termOpt = sentenceGenerator.generate();

      termOpt.ifPresent(System.out::println);
    }

    // TODO: pretty-print term
  }
}
