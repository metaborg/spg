package org.metaborg.spg.sentence;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.stratego.IStrategoCommon;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.HybridInterpreter;

public class PrettyPrinter {
  private static final String PRETTY_PRINT_STRATEGY = "pp-debug";
  private final IStrategoCommon stratego;
  private final HybridInterpreter interpreter;

  public PrettyPrinter(IStrategoCommon stratego, HybridInterpreter interpreter) {
    this.stratego = stratego;
    this.interpreter = interpreter;
  }

  public String prettyPrint(IStrategoTerm term) {
    try {
      IStrategoTerm program = stratego.invoke(interpreter, term, PRETTY_PRINT_STRATEGY);

      if (!(program instanceof IStrategoString)) {
        throw new IllegalStateException("The pretty-printer returned a non-string.");
      }

      return ((IStrategoString) program).stringValue();
    } catch (MetaborgException e) {
      throw new RuntimeException("Failed to pretty-print", e);
    }
  }
}
