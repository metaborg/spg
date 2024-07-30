package org.metaborg.spg.sentence.antlr.grammar;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

public class GrammarFactory {
    private final ISpoofaxUnitService unitService;
    private final ISpoofaxSyntaxService syntaxService;
    private final ISourceTextService sourceTextService;

    @jakarta.inject.Inject
    public GrammarFactory(ISpoofaxUnitService unitService, ISpoofaxSyntaxService syntaxService,
            ISourceTextService sourceTextService) {
        this.unitService = unitService;
        this.syntaxService = syntaxService;
        this.sourceTextService = sourceTextService;
    }

    public Grammar create(FileObject file, ILanguageImpl languageImpl) throws IOException, ParseException {
        String text = sourceTextService.text(file);

        return create(text, languageImpl);
    }

    public Grammar create(String text, ILanguageImpl languageImpl) throws IOException, ParseException {
        IStrategoTerm term = parse(text, languageImpl);

        return toGrammar(term);
    }

    protected IStrategoTerm parse(String text, ILanguageImpl languageImpl) throws ParseException {
        ISpoofaxInputUnit inputUnit = unitService.inputUnit(text, languageImpl, null);
        ISpoofaxParseUnit parseUnit = syntaxService.parse(inputUnit);

        return parseUnit.ast();
    }

    protected Grammar toGrammar(IStrategoTerm term) {
        String name = termToString(term.getSubterm(0));
        List<Rule> rules = toRules(term.getSubterm(1));

        return new Grammar(name, rules);
    }

    protected List<Rule> toRules(IStrategoTerm term) {
        return Arrays
                .stream(term.getAllSubterms())
                .map(this::toRule)
                .collect(Collectors.toList());
    }

    protected Rule toRule(IStrategoTerm term) {
        String name = termToString(term.getSubterm(1));
        EmptyElement emptyElement = toEmptyElement(term.getSubterm(2));

        return new Rule(name, emptyElement);
    }

    protected EmptyElement toEmptyElement(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        if ("Empty".equals(appl.getConstructor().getName())) {
            return new Empty();
        } else {
            return toElement(appl);
        }
    }

    protected Element toElement(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        switch (appl.getConstructor().getName()) {
            case "Conc":
                return toConc(appl);
            case "Alt":
                return toAlt(appl);
            case "Star":
                return toStar(appl);
            case "Plus":
                return toPlus(appl);
            case "Opt":
                return toOpt(appl);
            case "Not":
                return toNot(appl);
            case "Nonterminal":
                return toNonterminal(appl);
            case "Literal":
                return toLiteral(appl);
            case "DottedRange":
                return toDottedRange(appl);
            case "CharClass":
                return toCharacterClass(appl);
            case "NegClass":
                return toNegatedClass(appl);
            case "Command":
                return toCommand(appl);
            case "Wildcard":
                return toWildcard(appl);
            case "EOF":
                return toEOF(appl);
        }

        throw new IllegalArgumentException("Unknown element: " + appl);
    }

    protected Element toConc(IStrategoAppl appl) {
        Element first = toElement(appl.getSubterm(0));
        Element second = toElement(appl.getSubterm(1));

        return new Conc(first, second);
    }

    protected Element toAlt(IStrategoAppl appl) {
        EmptyElement first = toEmptyElement(appl.getSubterm(0));
        EmptyElement second = toEmptyElement(appl.getSubterm(1));

        return new Alt(first, second);
    }

    protected Element toStar(IStrategoAppl appl) {
        Element element = toElement(appl.getSubterm(0));

        return new Star(element);
    }

    protected Element toPlus(IStrategoAppl appl) {
        Element element = toElement(appl.getSubterm(0));

        return new Plus(element);
    }

    protected Element toOpt(IStrategoAppl appl) {
        Element element = toElement(appl.getSubterm(0));

        return new Opt(element);
    }

    private Element toNot(IStrategoAppl appl) {
        Element element = toElement(appl.getSubterm(0));

        return new Not(element);
    }

    protected Element toNonterminal(IStrategoAppl appl) {
        String name = termToString(appl.getSubterm(0));

        return new Nonterminal(name);
    }

    protected Element toLiteral(IStrategoAppl appl) {
        String text = unescape(unquote(termToString(appl.getSubterm(0))));

        return new Literal(text);
    }

    private Element toDottedRange(IStrategoAppl appl) {
        String start = unquote(termToString(appl.getSubterm(0)));
        String end = unquote(termToString(appl.getSubterm(1)));

        return new DottedRange(start, end);
    }

    private Element toCommand(IStrategoAppl appl) {
        Element element = toElement(appl.getSubterm(0));
        String name = termToString(appl.getSubterm(1));

        return new Command(element, name);
    }

    protected CharClass toCharacterClass(IStrategoAppl appl) {
        Ranges ranges = toRanges(appl.getSubterm(0));

        return new CharClass(ranges);
    }

    private Element toNegatedClass(IStrategoAppl appl) {
        CharacterClass characterClass = toCharacterClass((IStrategoAppl) appl.getSubterm(0));

        return new NegClass(characterClass);
    }

    private Element toWildcard(IStrategoAppl appl) {
        return new Wildcard();
    }

    protected Element toEOF(IStrategoAppl appl) {
        return new EOF();
    }

    protected Ranges toRanges(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        if ("RangeConc".equals(appl.getConstructor().getName())) {
            Ranges first = toRanges(term.getSubterm(0));
            Ranges second = toRanges(term.getSubterm(1));

            return new RangeConc(first, second);
        } else {
            return toRange(term);
        }
    }

    protected Range toRange(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        switch (appl.getConstructor().getName()) {
            case "CharRange":
                String first = termToString(appl.getSubterm(0));
                String second = termToString(appl.getSubterm(1));

                return new CharRange(first, second);
            case "Single":
                return toChar(term);
        }

        throw new IllegalArgumentException("Unknown range: " + term);
    }

    protected Character toChar(IStrategoTerm term) {
        String character = termToString(term.getSubterm(0));

        return new Single(character);
    }

    protected String termToString(IStrategoTerm term) {
        return ((IStrategoString) term).stringValue();
    }

    protected String unquote(String string) {
        return string.substring(1, string.length() - 1);
    }

    // TODO: Should we also unescape control characters (such as newline)?
    private String unescape(String string) {
        return string.replace("\\'", "'");
    }
}
