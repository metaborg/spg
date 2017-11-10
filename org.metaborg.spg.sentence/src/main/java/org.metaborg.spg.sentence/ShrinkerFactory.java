package org.metaborg.spg.sentence;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.sdf2table.grammar.Sort;
import org.metaborg.spg.sentence.signature.Operation;
import org.metaborg.spg.sentence.signature.Signature;
import org.metaborg.spoofax.core.build.SpoofaxCommonPaths;
import org.metaborg.spoofax.core.syntax.SyntaxFacet;
import org.spoofax.interpreter.terms.ITermFactory;

public class ShrinkerFactory {
  @Inject
  public ShrinkerFactory() {
  }

  public Shrinker create(ILanguageImpl language, IProject project, Generator generator, ITermFactory termFactory) {
    SpoofaxCommonPaths spoofaxCommonPaths = new SpoofaxCommonPaths(project.location());

//    SignatureReader signatureReader = new SignatureReader();
//    // TODO: SpoofaxCommonPaths.findSignatureMainFile should return ./signatures/{lang}-sig.str
//    Signature signature = signatureReader.read(null, spoofaxCommonPaths.syntaxSrcGenSignatureDir());
    Signature signature = createSignature();
    String rootSort = getRootSort(language);

    return new Shrinker(generator, termFactory, signature, rootSort);
  }

  // TODO: For the time being, we hard-code L0's signature here
  protected Signature createSignature() {
    List<Operation> operations = Arrays.asList(
        new Operation("IntValue", Arrays.asList(new Sort("IntValue")), new Sort("Exp")),
        new Operation("Var", Arrays.asList(new Sort("ID")), new Sort("Exp")),
        new Operation("Add", Arrays.asList(new Sort("Exp"), new Sort("Exp")), new Sort("Exp")),
        new Operation("Fun", Arrays.asList(new Sort("ID"), new Sort("Type"), new Sort("Exp")), new Sort("Exp")),
        new Operation("App", Arrays.asList(new Sort("Exp"), new Sort("Exp")), new Sort("Exp")),
        new Operation("IntType", Arrays.asList(), new Sort("Type")),
        new Operation("FunType", Arrays.asList(new Sort("Type"), new Sort("Type")), new Sort("Type"))
    );

    return new Signature(operations);
  }

  protected String getRootSort(ILanguageImpl language) {
    SyntaxFacet syntaxFacet = language.facet(SyntaxFacet.class);

    if (syntaxFacet == null) {
      throw new IllegalStateException("Unable to get syntax facet.");
    }

    return Iterables.get(syntaxFacet.startSymbols, 0);
  }
}
