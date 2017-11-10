package org.metaborg.spg.sentence;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class ParseService {
  private ISpoofaxSyntaxService syntaxService;
  private ISpoofaxUnitService unitService;

  @Inject
  public ParseService(ISpoofaxSyntaxService syntaxService, ISpoofaxUnitService unitService) {
    this.syntaxService = syntaxService;
    this.unitService = unitService;
  }

  public IStrategoTerm parse(ILanguageImpl language, String text) throws ParseException {
    ISpoofaxInputUnit inputUnit = unitService.inputUnit(text, language, null);
    ISpoofaxParseUnit parseUnit = syntaxService.parse(inputUnit);

    return parseUnit.ast();
  }

  public boolean isAmbiguous(IStrategoTerm term) {
    if (term instanceof IStrategoAppl) {
      IStrategoAppl appl = (IStrategoAppl) term;

      if ("amb".equals(appl.getConstructor().getName())) {
        return true;
      }
    }

    for (IStrategoTerm subterm : term.getAllSubterms()) {
      if (isAmbiguous(subterm)) {
        return true;
      }
    }

    return false;
  }
}
