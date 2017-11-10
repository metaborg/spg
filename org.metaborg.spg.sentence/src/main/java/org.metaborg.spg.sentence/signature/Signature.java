package org.metaborg.spg.sentence.signature;

import java.util.List;

import org.spoofax.interpreter.terms.IStrategoConstructor;

public class Signature {
  private final List<Operation> operations;

  public Signature(List<Operation> operations) {
    this.operations = operations;
  }

  public List<Operation> getOperations() {
    return operations;
  }

  public Operation getOperation(IStrategoConstructor constructor) {
    for (Operation operation : operations) {
      if (operation.matches(constructor)) {
        return operation;
      }
    }

    return null;
  }
}
