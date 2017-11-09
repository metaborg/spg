package org.metaborg.spg.sentence;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.metaborg.sdf2table.grammar.IProduction;
import org.metaborg.sdf2table.grammar.NormGrammar;
import org.metaborg.sdf2table.io.GrammarReader;
import org.spoofax.terms.TermFactory;

public class Main {
  public static void main(String[] args) throws Exception {
    TermFactory termFactory = new TermFactory();
    GrammarReader grammarReader = new GrammarReader(termFactory);

    //File input = new File("/tmp/metaborg-scopes-frames/L1/src-gen/syntax/normalized/Common-norm.aterm");
    File input = new File("/tmp/metaborg-scopes-frames/L1/src-gen/syntax/normalized/L1-norm.aterm");
    List<String> paths = Collections.emptyList();
    NormGrammar grammar = grammarReader.readGrammar(input, paths);

    System.out.println(grammar);

    for (IProduction production : grammar.getCacheProductionsRead().values()) {
      System.out.println(production);
    }

    // TODO: How to generate from the normalized grammar?
  }
}

