package org.metaborg.spg.sentence.sdf3;

import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spg.sentence.sdf3.attribute.Attribute;
import org.metaborg.spg.sentence.sdf3.attribute.Bracket;
import org.metaborg.spg.sentence.sdf3.attribute.Reject;
import org.metaborg.spg.sentence.sdf3.symbol.*;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.util.resource.FileSelectorUtils;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.metaborg.spg.sentence.shared.utils.FunctionalUtils.uncheck2;

public class GrammarFactory {
    private static final String SDF3_EXTENSION = "sdf3";

    private final ISpoofaxSyntaxService syntaxService;
    private final ISpoofaxUnitService unitService;
    private final ISourceTextService textService;

    @Inject
    public GrammarFactory(ISpoofaxSyntaxService syntaxService, ISourceTextService textService, ISpoofaxUnitService unitService) {
        this.syntaxService = syntaxService;
        this.textService = textService;
        this.unitService = unitService;
    }

    public Grammar fromProject(IProject project, ILanguageImpl languageImpl) throws IOException, ParseException {
        FileSelector fileSelector = FileSelectorUtils.extension(SDF3_EXTENSION);
        Iterable<FileObject> files = Arrays.asList(project.location().findFiles(fileSelector));

        Set<Module> modules = FluentIterable
                .from(files)
                .transform(uncheck2(file -> readModule(file, languageImpl)))
                .toSet();

        return new Grammar(modules);
    }

    public Module readModule(FileObject file, ILanguageImpl languageImpl) throws ParseException, IOException {
        String text = textService.text(file);
        ISpoofaxInputUnit input = unitService.inputUnit(file, text, languageImpl, null);
        ISpoofaxParseUnit parse = syntaxService.parse(input);

        return readModule(parse.ast());
    }

    private Module readModule(IStrategoTerm term) {
        String name = readString(term.getSubterm(0).getSubterm(0));
        Iterable<String> imports = readImports(term.getSubterm(1));
        Iterable<Section> sections = readSections(term.getSubterm(2));

        return new Module(name, imports, sections);
    }

    private Iterable<String> readImports(IStrategoTerm term) {
        IStrategoList list = (IStrategoList) term;

        if (list.size() == 0) {
            return Collections.emptySet();
        }

        Iterable<IStrategoTerm> modules = Arrays.asList(list.getSubterm(0).getSubterm(0).getAllSubterms());

        return FluentIterable
                .from(modules)
                .transform(this::readImport);
    }

    private String readImport(IStrategoTerm term) {
        return readString(term.getSubterm(0).getSubterm(0));
    }

    private Iterable<Section> readSections(IStrategoTerm term) {
        Iterable<IStrategoTerm> sections = Arrays.asList(term.getAllSubterms());

        return FluentIterable
                .from(sections)
                .transformAndConcat(this::readSection);
    }

    private Iterable<Section> readSection(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        if ("SDFSection".equals(appl.getConstructor().getName())) {
            return readSdfSection(term.getSubterm(0));
        }

        return Collections.emptySet();
    }

    private Iterable<Section> readSdfSection(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        if ("ContextFreeSyntax".equals(appl.getConstructor().getName())) {
            Iterable<Production> productions = readProductions(appl.getSubterm(0));

            return Collections.singleton(new ContextFreeSection(productions));
        }

        return Collections.emptySet();
    }

    private Iterable<Production> readProductions(IStrategoTerm term) {
        Iterable<IStrategoTerm> productions = Arrays.asList(term.getAllSubterms());

        return FluentIterable
                .from(productions)
                .transform(this::readProduction);
    }

    private Production readProduction(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        switch (appl.getConstructor().getName()) {
            case "SdfProduction":
                return readSdfProduction(appl);
            case "SdfProductionWithCons":
                return readSdfProductionWithCons(appl);
            case "TemplateProduction":
                return readTemplateProduction(appl);
            case "TemplateProductionWithCons":
                return readTemplateProductionWithCons(appl);
        }

        throw new IllegalArgumentException("Cannot read production from term: " + term);
    }

    private Production readSdfProduction(IStrategoAppl appl) {
        Nonterminal lhs = readNonterminal(appl.getSubterm(0));
        Iterable<Symbol> rhs = readRhs(appl.getSubterm(1));
        Iterable<Attribute> attributes = readAttributes(appl.getSubterm(2));

        return new Production(lhs, rhs, attributes);
    }

    private Production readSdfProductionWithCons(IStrategoAppl appl) {
        Nonterminal lhs = readNonterminal(appl.getSubterm(0).getSubterm(0));
        String constructor = readConstructor(appl.getSubterm(0).getSubterm(1));
        Iterable<Symbol> rhs = readRhs(appl.getSubterm(1));
        Iterable<Attribute> attributes = readAttributes(appl.getSubterm(2));

        return new Production(lhs, rhs, attributes, constructor);
    }

    private Production readTemplateProduction(IStrategoAppl appl) {
        Nonterminal lhs = readNonterminal(appl.getSubterm(0));
        Iterable<Symbol> rhs = readTemplate(appl.getSubterm(1));
        Iterable<Attribute> attributes = readAttributes(appl.getSubterm(2));

        return new Production(lhs, rhs, attributes);
    }

    private Production readTemplateProductionWithCons(IStrategoAppl appl) {
        Nonterminal lhs = readNonterminal(appl.getSubterm(0).getSubterm(0));
        String constructor = readConstructor(appl.getSubterm(0).getSubterm(1));
        Iterable<Symbol> rhs = readTemplate(appl.getSubterm(1));
        Iterable<Attribute> attributes = readAttributes(appl.getSubterm(2));

        return new Production(lhs, rhs, attributes, constructor);
    }

    private Nonterminal readNonterminal(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        if ("SortDef".equals(appl.getConstructor().getName())) {
            return new Nonterminal(readString(appl.getSubterm(0)));
        }

        throw new IllegalArgumentException("Cannot read nonterminal from term: " + term);
    }

    private String readConstructor(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        if ("Constructor".equals(appl.getConstructor().getName())) {
            return readString(appl.getSubterm(0));
        }

        throw new IllegalArgumentException("Cannot read constructor from term: " + term);
    }

    private Iterable<Symbol> readTemplate(IStrategoTerm term) {
        Iterable<IStrategoTerm> symbols = Arrays.asList(term.getSubterm(0).getAllSubterms());

        return FluentIterable
                .from(symbols)
                .transformAndConcat(this::readTemplateLine);
    }

    private Iterable<Symbol> readTemplateLine(IStrategoTerm term) {
        Iterable<IStrategoTerm> symbols = Arrays.asList(term.getSubterm(0).getAllSubterms());

        return FluentIterable
                .from(symbols)
                .transformAndConcat(this::readTemplatePart);
    }

    private Iterable<Symbol> readTemplatePart(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        switch (appl.getConstructor().getName()) {
            case "Angled":
            case "Squared":
                return Collections.singleton(readPlaceholder(appl.getSubterm(0)));
        }

        return Collections.emptySet();
    }

    private Symbol readPlaceholder(IStrategoTerm term) {
        return readSymbol(term.getSubterm(0));
    }

    private Iterable<Symbol> readRhs(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        if ("Rhs".equals(appl.getConstructor().getName())) {
            return readSymbols(appl.getSubterm(0));
        }

        throw new IllegalArgumentException("Cannot read rhs from term: " + term);
    }

    private Iterable<Symbol> readSymbols(IStrategoTerm term) {
        Iterable<IStrategoTerm> productions = Arrays.asList(term.getAllSubterms());

        return FluentIterable
                .from(productions)
                .transform(this::readSymbol);
    }

    private Symbol readSymbol(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        switch (appl.getConstructor().getName()) {
            case "Sort":
                return new Nonterminal(readString(appl.getSubterm(0)));
            case "Lit":
                return new Literal(unquote(readString(appl.getSubterm(0))));
            case "Iter":
                return new Iter(readSymbol(appl.getSubterm(0)));
            case "IterSep":
                return new IterSep(readSymbol(appl.getSubterm(0)), readSymbol(appl.getSubterm(1)));
            case "IterStar":
                return new IterStar(readSymbol(appl.getSubterm(0)));
            case "IterStarSep":
                return new IterStarSep(readSymbol(appl.getSubterm(0)), readSymbol(appl.getSubterm(1)));
            case "Opt":
                return new Opt(readSymbol(appl.getSubterm(0)));
        }

        throw new IllegalArgumentException("Cannot read symbol from term: " + term);
    }

    private Iterable<Attribute> readAttributes(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        switch (appl.getConstructor().getName()) {
            case "NoAttrs":
                return Collections.emptySet();
            case "Attrs":
                return readAttributesList(term);
        }

        throw new IllegalArgumentException("Cannot read attributes from term: " + term);
    }

    private Iterable<Attribute> readAttributesList(IStrategoTerm term) {
        Iterable<IStrategoTerm> attributes = Arrays.asList(term.getSubterm(0).getAllSubterms());

        return FluentIterable
                .from(attributes)
                .transformAndConcat(this::readAttribute);
    }

    private Iterable<Attribute> readAttribute(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        switch (appl.getConstructor().getName()) {
            case "Bracket":
                return Collections.singleton(new Bracket());
            case "Reject":
                return Collections.singleton(new Reject());
        }

        return Collections.emptySet();
    }

    private String readString(IStrategoTerm term) {
        if (!(term instanceof IStrategoString)) {
            throw new IllegalArgumentException("Cannot read string from term: " + term);
        }

        IStrategoString string = (IStrategoString) term;

        return unescape(string.stringValue());
    }

    private String unquote(String text) {
        return text.substring(1, text.length() - 1);
    }

    private String unescape(String text) {
        return text
                .replaceAll("\\\\([^0-9a-zA-Z])", "$1")
                .replace("\\\n", "\n")
                .replace("\\\r", "\r")
                .replace("\\\t", "\t");
    }
}
