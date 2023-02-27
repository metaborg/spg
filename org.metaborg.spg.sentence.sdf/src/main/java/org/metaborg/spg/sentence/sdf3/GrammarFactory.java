package org.metaborg.spg.sentence.sdf3;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spg.sentence.sdf3.attribute.Attribute;
import org.metaborg.spg.sentence.sdf3.attribute.Bracket;
import org.metaborg.spg.sentence.sdf3.attribute.Reject;
import org.metaborg.spg.sentence.sdf3.symbol.Iter;
import org.metaborg.spg.sentence.sdf3.symbol.IterSep;
import org.metaborg.spg.sentence.sdf3.symbol.IterStar;
import org.metaborg.spg.sentence.sdf3.symbol.IterStarSep;
import org.metaborg.spg.sentence.sdf3.symbol.Literal;
import org.metaborg.spg.sentence.sdf3.symbol.Nonterminal;
import org.metaborg.spg.sentence.sdf3.symbol.Opt;
import org.metaborg.spg.sentence.sdf3.symbol.Symbol;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.util.resource.FileSelectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

import static org.metaborg.spg.sentence.shared.utils.FunctionalUtils.uncheck2;

public class GrammarFactory {
    private static final Logger logger = LoggerFactory.getLogger(GrammarFactory.class);
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

    public Grammar create(ILanguageImpl languageImpl, IProject project) throws IOException, ParseException {
        logger.trace("Read SDF grammar");

        FileSelector fileSelector = FileSelectorUtils.extension(SDF3_EXTENSION);
        List<FileObject> files;
        try(final FileObject location = project.location()) {
            files = Arrays.asList(location.findFiles(fileSelector));
        }

        Set<Module> modules = files.stream().map(uncheck2(file -> readModule(file, languageImpl))).collect(Collectors.toSet());

        return new Grammar(modules);
    }

    public Module readModule(FileObject file, ILanguageImpl languageImpl) throws ParseException, IOException {
        logger.trace("Read SDF3 module " + file);

        String text = textService.text(file);
        ISpoofaxInputUnit input = unitService.inputUnit(file, text, languageImpl, null);
        ISpoofaxParseUnit parse = syntaxService.parse(input);

        return readModule(Objects.requireNonNull(parse.ast()));
    }

    private Module readModule(IStrategoTerm term) {
        String name = readString(term.getSubterm(0).getSubterm(0));
        Collection<String> imports = readImports(term.getSubterm(1));
        Collection<Section> sections = readSections(term.getSubterm(2));

        return new Module(name, imports, sections);
    }

    private Collection<String> readImports(IStrategoTerm term) {
        IStrategoList list = (IStrategoList) term;

        if (list.size() == 0) {
            return Collections.emptySet();
        }

        List<IStrategoTerm> modules = Arrays.asList(list.getSubterm(0).getSubterm(0).getAllSubterms());

        return modules.stream().map(this::readImport).collect(Collectors.toList());
    }

    private String readImport(IStrategoTerm term) {
        return readString(term.getSubterm(0).getSubterm(0));
    }

    private Collection<Section> readSections(IStrategoTerm term) {
        List<IStrategoTerm> sections = Arrays.asList(term.getAllSubterms());

        return sections.stream()
                .flatMap(this::readSection).collect(Collectors.toList());
    }

    private Stream<Section> readSection(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        if ("SDFSection".equals(appl.getConstructor().getName())) {
            return readSdfSection(term.getSubterm(0));
        }

        return Stream.empty();
    }

    private Stream<Section> readSdfSection(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        if ("ContextFreeSyntax".equals(appl.getConstructor().getName())) {
            Collection<Production> productions = readProductions(appl.getSubterm(0));

            return Stream.of(new ContextFreeSection(productions));
        }

        return Stream.empty();
    }

    private Collection<Production> readProductions(IStrategoTerm term) {
        Collection<IStrategoTerm> productions = Arrays.asList(term.getAllSubterms());

        return productions.stream().map(this::readProduction).collect(Collectors.toList());
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
        Stream<Symbol> rhs = readRhs(appl.getSubterm(1));
        Stream<Attribute> attributes = readAttributes(appl.getSubterm(2));

        return new Production(lhs, rhs, attributes);
    }

    private Production readSdfProductionWithCons(IStrategoAppl appl) {
        Nonterminal lhs = readNonterminal(appl.getSubterm(0).getSubterm(0));
        String constructor = readConstructor(appl.getSubterm(0).getSubterm(1));
        Stream<Symbol> rhs = readRhs(appl.getSubterm(1));
        Stream<Attribute> attributes = readAttributes(appl.getSubterm(2));

        return new Production(lhs, rhs, attributes, constructor);
    }

    private Production readTemplateProduction(IStrategoAppl appl) {
        Nonterminal lhs = readNonterminal(appl.getSubterm(0));
        Stream<Symbol> rhs = readTemplate(appl.getSubterm(1));
        Stream<Attribute> attributes = readAttributes(appl.getSubterm(2));

        return new Production(lhs, rhs, attributes);
    }

    private Production readTemplateProductionWithCons(IStrategoAppl appl) {
        Nonterminal lhs = readNonterminal(appl.getSubterm(0).getSubterm(0));
        String constructor = readConstructor(appl.getSubterm(0).getSubterm(1));
        Stream<Symbol> rhs = readTemplate(appl.getSubterm(1));
        Stream<Attribute> attributes = readAttributes(appl.getSubterm(2));

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

    private Stream<Symbol> readTemplate(IStrategoTerm term) {
        Collection<IStrategoTerm> symbols = Arrays.asList(term.getSubterm(0).getAllSubterms());

        return symbols.stream().flatMap(this::readTemplateLine);
    }

    private Stream<Symbol> readTemplateLine(IStrategoTerm term) {
        Collection<IStrategoTerm> symbols = Arrays.asList(term.getSubterm(0).getAllSubterms());

        return symbols.stream().flatMap(this::readTemplatePart);
    }

    private Stream<Symbol> readTemplatePart(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        switch (appl.getConstructor().getName()) {
            case "Angled":
            case "Squared":
                return Stream.of(readPlaceholder(appl.getSubterm(0)));
        }

        return Stream.empty();
    }

    private Symbol readPlaceholder(IStrategoTerm term) {
        return readSymbol(term.getSubterm(0));
    }

    private Stream<Symbol> readRhs(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        if ("Rhs".equals(appl.getConstructor().getName())) {
            return readSymbols(appl.getSubterm(0));
        }

        throw new IllegalArgumentException("Cannot read rhs from term: " + term);
    }

    private Stream<Symbol> readSymbols(IStrategoTerm term) {
        Collection<IStrategoTerm> productions = Arrays.asList(term.getAllSubterms());

        return productions.stream().map(this::readSymbol);
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

    private Stream<Attribute> readAttributes(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        switch (appl.getConstructor().getName()) {
            case "NoAttrs":
                return Stream.empty();
            case "Attrs":
                return readAttributesList(term);
        }

        throw new IllegalArgumentException("Cannot read attributes from term: " + term);
    }

    private Stream<Attribute> readAttributesList(IStrategoTerm term) {
        Collection<IStrategoTerm> attributes = Arrays.asList(term.getSubterm(0).getAllSubterms());

        return attributes.stream().flatMap(this::readAttribute);
    }

    private Stream<Attribute> readAttribute(IStrategoTerm term) {
        IStrategoAppl appl = (IStrategoAppl) term;

        switch (appl.getConstructor().getName()) {
            case "Bracket":
                return Stream.of(new Bracket());
            case "Reject":
                return Stream.of(new Reject());
        }

        return Stream.empty();
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
