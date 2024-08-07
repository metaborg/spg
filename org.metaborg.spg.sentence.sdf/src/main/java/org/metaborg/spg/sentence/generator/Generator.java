package org.metaborg.spg.sentence.generator;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.metaborg.parsetable.characterclasses.CharacterClassFactory;
import org.metaborg.parsetable.characterclasses.ICharacterClass;
import org.metaborg.sdf2table.grammar.*;
import org.metaborg.sdf2table.io.ParseTableIO;
import org.metaborg.spg.sentence.random.IRandom;
import org.metaborg.spg.sentence.terms.GeneratorTermFactory;
import org.metaborg.util.collection.MultiSetMap;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

public class Generator {
    public static final int MINIMUM_PRINTABLE = 32;
    public static final int MAXIMUM_PRINTABLE = 126;

    private final GeneratorTermFactory termFactory;
    private final IRandom random;
    private final String startSymbol;
    private final NormGrammar grammar;
    private final MultiSetMap.Immutable<ISymbol, IProduction> productionsMap;
    private final GrammarFactory gf;

    @jakarta.inject.Inject public Generator(GeneratorTermFactory termFactory, IRandom random, String startSymbol,
        NormGrammar grammar) {
        this.termFactory = termFactory;
        this.random = random;
        this.startSymbol = startSymbol;
        this.grammar = grammar;
        this.gf = new GrammarFactory();

        Collection<IProduction> productions = retainRealProductions(grammar.getCacheProductionsRead().values());
        this.productionsMap = createProductionMap(productions);
    }

    public Optional<IStrategoTerm> generate(int size) {
        ContextFreeSymbol symbol = gf.createContextFreeSymbol(gf.createSort(startSymbol));
        return generateSymbol(symbol, size);
    }

    public Optional<IStrategoTerm> generateSymbol(ISymbol symbol, int size) {
        if(size <= 0) {
            return Optional.empty();
        }

        if(symbol instanceof LexicalSymbol) {
            String generatedString = generateLexicalSymbol(symbol);

            return Optional.of(termFactory.makeString(symbol, generatedString));
        } else if(symbol instanceof ContextFreeSymbol) {
            Symbol innerSymbol = ((ContextFreeSymbol) symbol).getSymbol();

            if(innerSymbol instanceof IterSymbol) {
                return generateIter(gf.createContextFreeSymbol(((IterSymbol) innerSymbol).getSymbol()), size);
            } else if(innerSymbol instanceof IterSepSymbol) {
                return generateIter(gf.createContextFreeSymbol(((IterSepSymbol) innerSymbol).getSymbol()), size);
            } else if(innerSymbol instanceof IterStarSymbol) {
                return generateIterStar(gf.createContextFreeSymbol(((IterStarSymbol) innerSymbol).getSymbol()), size);
            } else if(innerSymbol instanceof IterStarSepSymbol) {
                return generateIterStar(gf.createContextFreeSymbol(((IterStarSepSymbol) innerSymbol).getSymbol()), size);
            } else if(innerSymbol instanceof OptionalSymbol) {
                return generateOptional(gf.createContextFreeSymbol(((OptionalSymbol) innerSymbol).getSymbol()), size);
            } else if(innerSymbol instanceof Sort) {
                return generateCf(symbol, size);
            }
        } else {
            return generateCf(symbol, size);
        }

        throw new IllegalStateException("Unknown symbol: " + symbol);
    }

    private Optional<IStrategoTerm> generateIterStar(Symbol symbol, int size) {
        IterStarSymbol iterStarSymbol = gf.createIterStarSymbol(symbol);

        if(random.flip()) {
            return Optional.of(termFactory.makeList(iterStarSymbol));
        } else {
            Optional<IStrategoTerm> headOpt = generateSymbol(symbol, size / 2);

            if(headOpt.isPresent()) {
                Optional<IStrategoTerm> tailOpt = generateIterStar(symbol, size / 2);

                if(tailOpt.isPresent()) {
                    IStrategoTerm head = headOpt.get();
                    IStrategoList tail = (IStrategoList) tailOpt.get();
                    IStrategoList list = termFactory.makeListCons(iterStarSymbol, head, tail);

                    return Optional.of(list);
                }
            }
        }

        return Optional.empty();
    }

    private Optional<IStrategoTerm> generateIter(Symbol symbol, int size) {
        IterSymbol iterSymbol = gf.createIterSymbol(symbol);
        Optional<IStrategoTerm> headOpt = generateSymbol(symbol, size / 2);

        if(headOpt.isPresent()) {
            Optional<IStrategoTerm> tailOpt = generateIterStar(symbol, size / 2);

            if(tailOpt.isPresent()) {
                IStrategoTerm head = headOpt.get();
                IStrategoList tail = (IStrategoList) tailOpt.get();

                return Optional.of(termFactory.makeListCons(iterSymbol, head, tail));
            }
        }

        return Optional.empty();
    }

    private Optional<IStrategoTerm> generateOptional(Symbol symbol, int size) {
        OptionalSymbol optionalSymbol = gf.createOptionalSymbol(symbol);

        if(random.flip()) {
            return Optional.of(termFactory.makeNone(optionalSymbol));
        } else {
            Optional<IStrategoTerm> termOpt = generateSymbol(symbol, size - 1);

            return termOpt.map(term -> termFactory.makeSome(optionalSymbol, term));
        }
    }

    public String generateLex(ISymbol symbol) {
        if(symbol instanceof CharacterClassSymbol) {
            return generateCharacterClass(((CharacterClassSymbol) symbol));
        } else if(symbol instanceof LexicalSymbol || symbol instanceof Sort) {
            return generateLexicalSymbol(symbol);
        }

        throw new IllegalStateException("Unknown symbol: " + symbol);
    }

    public String generateCharacterClass(CharacterClassSymbol characterClassSymbol) {
        ICharacterClass printableRange = characterClassSymbol.getCC()
            .intersection(ParseTableIO.getCharacterClassFactory().fromRange(MINIMUM_PRINTABLE, MAXIMUM_PRINTABLE));

        List<Integer> list = new ArrayList<>(MAXIMUM_PRINTABLE);
        for(int i = MINIMUM_PRINTABLE; i <= MAXIMUM_PRINTABLE; i++) {
            if(printableRange.contains(i)) {
                list.add(i);
            }
        }

        if(list.size() == 0)
            return "";

        return CharacterClassFactory.intToString(random.fromList(list));
    }

    public String generateLexicalSymbol(ISymbol symbol) {
        List<IProduction> productions = new ArrayList<>(productionsMap.get(symbol).toCollection());

        if(productions.isEmpty()) {
            throw new IllegalStateException("No productions found for symbol " + symbol);
        }

        IProduction production = random.fromList(productions);

        return production.rightHand().stream().map(this::generateLex).collect(Collectors.joining());
    }

    public Optional<IStrategoTerm> generateCf(ISymbol symbol, int size) {
        List<IProduction> productions = new ArrayList<>(productionsMap.get(symbol).toCollection());

        for(IProduction production : random.shuffle(productions)) {
            Optional<IStrategoTerm> term = generateProduction(production, size);

            if(term.isPresent()) {
                return term;
            }
        }

        return Optional.empty();
    }

    public Optional<IStrategoTerm> generateProduction(IProduction production, int size) {
        List<ISymbol> rhsSymbols = cleanRhs(production.rightHand());
        List<IStrategoTerm> children = new ArrayList<>();

        int childSize = (size - 1) / Math.max(1, rhsSymbols.size());

        for(ISymbol rhsSymbol : rhsSymbols) {
            Optional<IStrategoTerm> childTerm = generateSymbol(rhsSymbol, childSize);

            if(childTerm.isPresent()) {
                children.add(childTerm.get());
            } else {
                break;
            }
        }

        if(children.size() == rhsSymbols.size()) {
            Optional<String> constructor = getConstructor(production);

            if(constructor.isPresent()) {
                return Optional.of(termFactory.makeAppl(production.leftHand(), constructor.get(), children));
            } else {
                if(children.size() == 0) {
                    return Optional.empty();
                }

                return Optional.of(children.get(0));
            }
        }

        return Optional.empty();
    }

    protected Optional<String> getConstructor(IProduction production) {
        Optional<IAttribute> findAttribute = findAttribute(production, this::isConstructorAttribute);

        return findAttribute.map(attribute -> ((ConstructorAttribute) attribute).getConstructor());
    }

    protected boolean isConstructorAttribute(IAttribute attribute) {
        return attribute instanceof ConstructorAttribute;
    }

    protected List<ISymbol> cleanRhs(List<ISymbol> rightHand) {
        return rightHand.stream().filter(this::isProperSymbol).collect(Collectors.toList());
    }

    protected boolean isProperSymbol(ISymbol symbol) {
        if("LAYOUT?-CF".equals(symbol.name())) {
            return false;
        }

        // @formatter:off
        return symbol instanceof ContextFreeSymbol
            || symbol instanceof FileStartSymbol
            || symbol instanceof StartSymbol
            || symbol instanceof EOFSymbol
            || symbol instanceof LexicalSymbol;
        // @formatter:on
    }

    protected MultiSetMap.Immutable<ISymbol, IProduction> createProductionMap(Collection<IProduction> productions) {
        MultiSetMap.Transient<ISymbol, IProduction> productionsMap = MultiSetMap.Transient.of();

        for(IProduction production : productions) {
            productionsMap.put(production.leftHand(), production);
        }

        return productionsMap.freeze();
    }

    protected Collection<IProduction> retainRealProductions(Collection<Production> collection) {
        return collection.stream().filter(this::isRealProduction).collect(Collectors.toList());
    }

    protected boolean isRealProduction(IProduction production) {
        return !isPlaceholder(production) && !isRecover(production) && !isReject(production);
    }

    protected boolean isPlaceholder(IProduction production) {
        return findAttribute(production, this::isPlaceholderAttribute).isPresent();
    }

    protected boolean isPlaceholderAttribute(IAttribute attribute) {
        return isAttribute(attribute, "placeholder");
    }

    protected boolean isRecover(IProduction production) {
        return findAttribute(production, this::isRecoverAttribute).isPresent();
    }

    protected boolean isRecoverAttribute(IAttribute attribute) {
        return isAttribute(attribute, "recover");
    }

    protected boolean isReject(IProduction production) {
        return findAttribute(production, this::isRejectAttribute).isPresent();
    }

    protected boolean isRejectAttribute(IAttribute attribute) {
        return isAttribute(attribute, "reject");
    }

    protected boolean isAttribute(IAttribute attribute, String name) {
        if(attribute instanceof GeneralAttribute) {
            GeneralAttribute generalAttribute = (GeneralAttribute) attribute;

            if(name.equals(generalAttribute.getName())) {
                return true;
            }
        }

        return false;
    }

    protected Optional<IAttribute> findAttribute(IProduction production, Predicate<IAttribute> predicate) {
        Set<IAttribute> attributes = grammar.getProductionAttributesMapping().get(production);

        for(IAttribute attribute : attributes) {
            if(predicate.test(attribute)) {
                return Optional.of(attribute);
            }
        }

        return Optional.empty();
    }
}
