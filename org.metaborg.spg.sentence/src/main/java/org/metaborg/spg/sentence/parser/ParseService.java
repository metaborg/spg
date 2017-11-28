package org.metaborg.spg.sentence.parser;

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

    public IStrategoTerm parse(ILanguageImpl language, String text) {
        return parseUnit(language, text).ast();
    }

    public ISpoofaxParseUnit parseUnit(ILanguageImpl language, String text) {
        ISpoofaxInputUnit inputUnit = unitService.inputUnit(text, language, null);

        try {
            ISpoofaxParseUnit parseUnit = syntaxService.parse(inputUnit);

            if (!parseUnit.success()) {
                throw new ParseRuntimeException("Unable to parse: " + text);
            }

            return parseUnit;
        } catch (ParseException e) {
            throw new ParseRuntimeException("Unable to parse: " + text, e);
        }
    }
}
