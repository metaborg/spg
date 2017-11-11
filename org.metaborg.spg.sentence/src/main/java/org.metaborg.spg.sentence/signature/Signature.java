package org.metaborg.spg.sentence.signature;

import java.util.List;

import org.spoofax.interpreter.terms.IStrategoConstructor;

public class Signature {
  private final List<Constructor> constructors;

  public Signature(List<Constructor> constructors) {
    this.constructors = constructors;
  }

  public List<Constructor> getConstructors() {
    return constructors;
  }

  public Constructor getOperation(IStrategoConstructor constructor) {
    for (Constructor operation : constructors) {
      if (operation.matches(constructor)) {
        return operation;
      }
    }

    return null;
  }
}
