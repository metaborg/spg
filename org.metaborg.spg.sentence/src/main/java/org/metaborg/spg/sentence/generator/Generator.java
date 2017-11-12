package org.metaborg.spg.sentence.generator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import org.metaborg.sdf2table.grammar.*;
import org.metaborg.spg.sentence.printer.Printer;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.StrategoConstructor;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Generator {
    public static Random random = new Random(0);

    private final Printer printer;
    private final ITermFactory termFactory;
    private final NormGrammar grammar;
    private final Collection<IProduction> productions;
    private final ListMultimap<Symbol, IProduction> productionsMap;

    @Inject
    public Generator(Printer printer, ITermFactory termFactory, NormGrammar grammar) {
        this.printer = printer;
        this.termFactory = termFactory;
        this.grammar = grammar;
        this.productions = retainRealProductions(grammar.getCacheProductionsRead().values());
        this.productionsMap = createProductionMap(productions);
    }

    public Optional<String> generate(int size) {
        IProduction initialProduction = grammar.getInitialProduction();
        Optional<IStrategoTerm> termOpt = generateTerm(initialProduction.leftHand(), size);

        return termOpt.map(printer::print);
    }

    public Optional<IStrategoTerm> generateTerm(int size) {
        IProduction initialProduction = grammar.getInitialProduction();

        return generateTerm(initialProduction.leftHand(), size);
    }

    public Optional<IStrategoTerm> generateTerm(Symbol symbol, int size) {
        if (symbol.name().endsWith("-LEX")) {
            return Optional.of(termFactory.makeString(generateLex(symbol)));
        } else {
            return generateCf(symbol, size);
        }
    }

    public String generateLex(Symbol symbol) {
        if (symbol instanceof CharacterClass) {
            return generateLex(((CharacterClass) symbol).symbol());
        } else if (symbol instanceof CharacterClassConc) {
            return generateCharacterClassConc((CharacterClassConc) symbol);
        } else if (symbol instanceof CharacterClassRange) {
            return generateCharacterClassRange((CharacterClassRange) symbol);
        } else if (symbol instanceof LexicalSymbol || symbol instanceof Sort) {
            return generateLexicalSymbol(symbol);
        } else if (symbol instanceof CharacterClassNumeric) {
            return generateCharacterClassNumeric((CharacterClassNumeric) symbol);
        }

        throw new IllegalStateException("Unknown symbol: " + symbol);
    }

    public String generateCharacterClassConc(CharacterClassConc characterClassConc) {
        String first = generateLex(characterClassConc.first());
        String second = generateLex(characterClassConc.second());

        return first + second;
    }

    public String generateCharacterClassRange(CharacterClassRange characterClassRange) {
        int range = characterClassRange.maximum() - characterClassRange.minimum() + 1;
        char character = (char) (characterClassRange.minimum() + random.nextInt(range));

        return String.valueOf(character);
    }

    public String generateCharacterClassNumeric(CharacterClassNumeric characterClassNumeric) {
        return String.valueOf(characterClassNumeric.getCharacter());
    }

    public String generateLexicalSymbol(Symbol symbol) {
        List<IProduction> productions = productionsMap.get(symbol);

        if (productions.isEmpty()) {
            throw new IllegalStateException("No productions found for symbol " + symbol);
        }

        IProduction production = random(productions);

        return production.rightHand().stream().map(this::generateLex).collect(Collectors.joining());
    }

    public Optional<IStrategoTerm> generateCf(Symbol symbol, int size) {
        if (size <= 0) {
            return Optional.empty();
        }

        List<IProduction> productions = productionsMap.get(symbol);

        if (productions.isEmpty()) {
            return Optional.empty();
        }

        IProduction production = random(productions);
        List<Symbol> symbols = cleanRhs(production.rightHand());
        int childSize = (size - 1) / Math.max(1, symbols.size());
        List<IStrategoTerm> children = new ArrayList<>();

        for (Symbol rhsSymbol : symbols) {
            Optional<IStrategoTerm> childTerm = generateTerm(rhsSymbol, childSize);

            if (!childTerm.isPresent()) {
                return Optional.empty();
            } else {
                children.add(childTerm.get());
            }
        }

        Optional<String> constructor = getConstructor(production);

        if (constructor.isPresent()) {
            return Optional.of(makeAppl(constructor.get(), children));
        } else {
            return Optional.of(children.get(0));
        }
    }

    protected <T> T random(List<T> list) {
        return list.get(new java.util.Random().nextInt(list.size()));
    }

    /**
     * Create a term based on the given constructor and child terms.
     *
     * @param constructorName
     * @param children
     * @return
     */
    protected IStrategoTerm makeAppl(String constructorName, List<IStrategoTerm> children) {
        IStrategoConstructor constructor = new StrategoConstructor(constructorName, children.size());
        IStrategoTerm[] terms = new IStrategoTerm[children.size()];

        return termFactory.makeAppl(constructor, children.toArray(terms));
    }

    /**
     * Get the constructor for the given attribute.
     *
     * @param production
     * @return
     */
    protected Optional<String> getConstructor(IProduction production) {
        Optional<IAttribute> findAttribute = findAttribute(production, this::isConstructorAttribute);

        return findAttribute.map(attribute -> ((ConstructorAttribute) attribute).getConstructor());
    }

    /**
     * Check if the given attribute is a constructor attribute.
     *
     * @param attribute
     * @return
     */
    protected boolean isConstructorAttribute(IAttribute attribute) {
        return attribute instanceof ConstructorAttribute;
    }

    /**
     * Remove LAYOUT from a list of symbols (e.g. the right-hand side of a production).
     *
     * @param rightHand
     * @return
     */
    protected List<Symbol> cleanRhs(List<Symbol> rightHand) {
        return rightHand.stream().filter(
                symbol -> !"LAYOUT?-CF".equals(symbol.name()) && (symbol instanceof ContextFreeSymbol
                        || symbol instanceof FileStartSymbol || symbol instanceof StartSymbol || symbol instanceof LexicalSymbol))
                .collect(Collectors.toList());
    }

    /**
     * Create a multimap from left-hand symbol to production.
     *
     * @param productions
     * @return
     */
    protected ListMultimap<Symbol, IProduction> createProductionMap(Collection<IProduction> productions) {
        ListMultimap<Symbol, IProduction> productionsMap = ArrayListMultimap.create();

        for (IProduction production : productions) {
            productionsMap.put(production.leftHand(), production);
        }

        return productionsMap;
    }

    protected Collection<IProduction> retainRealProductions(Collection<IProduction> productions) {
        return productions.stream().filter(this::isRealProduction).collect(Collectors.toList());
    }

    protected boolean isRealProduction(IProduction production) {
        return !isPlaceholder(production) && !isRecover(production);
    }

    protected boolean isPlaceholder(IProduction production) {
        Optional<IAttribute> findAttribute = findAttribute(production, this::isPlaceholderAttribute);

        return findAttribute.isPresent();
    }

    protected boolean isPlaceholderAttribute(IAttribute attribute) {
        if (attribute instanceof GeneralAttribute) {
            GeneralAttribute generalAttribute = (GeneralAttribute) attribute;

            if ("placeholder".equals(generalAttribute.getName())) {
                return true;
            }
        }

        return false;
    }

    protected boolean isRecover(IProduction production) {
        Optional<IAttribute> findAttribute = findAttribute(production, this::isRecoverAttribute);

        return findAttribute.isPresent();
    }

    protected boolean isRecoverAttribute(IAttribute attribute) {
        if (attribute instanceof GeneralAttribute) {
            GeneralAttribute generalAttribute = (GeneralAttribute) attribute;

            if ("recover".equals(generalAttribute.getName())) {
                return true;
            }
        }

        return false;
    }

    protected Optional<IAttribute> findAttribute(IProduction production, Predicate<IAttribute> predicate) {
        Set<IAttribute> attributes = grammar.getProductionAttributesMapping().get(production);

        for (IAttribute attribute : attributes) {
            if (predicate.test(attribute)) {
                return Optional.of(attribute);
            }
        }

        return Optional.empty();
    }
}
