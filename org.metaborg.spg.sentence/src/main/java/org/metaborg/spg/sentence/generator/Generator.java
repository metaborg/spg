package org.metaborg.spg.sentence.generator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import org.metaborg.sdf2table.grammar.*;
import org.metaborg.spg.sentence.Utils;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.StrategoConstructor;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Generator {
    public static final int MINIMUM_PRINTABLE = 32;
    public static final int MAXIMUM_PRINTABLE = 126;
    public static final Random random = new Random();

    private final ITermFactory termFactory;
    private final String startSymbol;
    private final NormGrammar grammar;
    private final Collection<IProduction> productions;
    private final ListMultimap<Symbol, IProduction> productionsMap;

    @Inject
    public Generator(ITermFactory termFactory, String startSymbol, NormGrammar grammar) {
        this.termFactory = termFactory;
        this.startSymbol = startSymbol;
        this.grammar = grammar;
        this.productions = retainRealProductions(grammar.getCacheProductionsRead().values());
        this.productionsMap = createProductionMap(productions);
    }

    public Optional<IStrategoTerm> generate(int size) {
        ContextFreeSymbol symbol = new ContextFreeSymbol(new Sort(startSymbol));

        return generateSymbol(symbol, size);
    }

    public Optional<IStrategoTerm> generateSymbol(Symbol symbol, int size) {
        if (size <= 0) {
            return Optional.empty();
        }

        if (symbol instanceof LexicalSymbol) {
            String generatedString = generateLex(symbol);

            return Optional.of(termFactory.makeString(generatedString));
        } else if (symbol instanceof ContextFreeSymbol) {
            Symbol innerSymbol = ((ContextFreeSymbol) symbol).getSymbol();

            if (innerSymbol instanceof IterSymbol) {
                return generateIter(new ContextFreeSymbol(((IterSymbol) innerSymbol).getSymbol()), size);
            } else if (innerSymbol instanceof IterSepSymbol) {
                return generateIter(new ContextFreeSymbol(((IterSepSymbol) innerSymbol).getSymbol()), size);
            } else if (innerSymbol instanceof IterStarSymbol) {
                return generateIterStar(new ContextFreeSymbol(((IterStarSymbol) innerSymbol).getSymbol()), size);
            } else if (innerSymbol instanceof IterStarSepSymbol) {
                return generateIterStar(new ContextFreeSymbol(((IterStarSepSymbol) innerSymbol).getSymbol()), size);
            } else if (innerSymbol instanceof OptionalSymbol) {
                return generateOptional(new ContextFreeSymbol(((OptionalSymbol) innerSymbol).getSymbol()), size);
            } else if (innerSymbol instanceof Sort) {
                return generateCf(symbol, size);
            }
        } else {
            return generateCf(symbol, size);
        }

        throw new IllegalStateException("Unknown symbol: " + symbol);
    }

    private Optional<IStrategoTerm> generateIterStar(Symbol symbol, int size) {
        if (random.nextInt(2) == 0) {
            return Optional.of(termFactory.makeList());
        } else {
            Optional<IStrategoTerm> headOpt = generateSymbol(symbol, size);

            if (headOpt.isPresent()) {
                Optional<IStrategoTerm> tailOpt = generateIter(symbol, size);

                if (tailOpt.isPresent()) {
                    return Optional.of(termFactory.makeListCons(headOpt.get(), (IStrategoList) tailOpt.get()));
                }
            }
        }

        return Optional.empty();
    }

    private Optional<IStrategoTerm> generateIter(Symbol symbol, int size) {
        Optional<IStrategoTerm> headOpt = generateSymbol(symbol, size / 2);

        if (headOpt.isPresent()) {
            Optional<IStrategoTerm> tailOpt = generateIterStar(symbol, size / 2);

            if (tailOpt.isPresent()) {
                return Optional.of(termFactory.makeListCons(headOpt.get(), (IStrategoList) tailOpt.get()));
            }
        }

        return Optional.empty();
    }

    private Optional<IStrategoTerm> generateOptional(Symbol symbol, int size) {
        if (random.nextInt(2) == 0) {
            return Optional.of(makeNone());
        } else {
            Optional<IStrategoTerm> term = generateSymbol(symbol, size - 1);

            return term.map(this::makeSome);
        }
    }

    private IStrategoTerm makeSome(IStrategoTerm term) {
        return termFactory.makeAppl(termFactory.makeConstructor("Some", 1), term);
    }

    private IStrategoTerm makeNone() {
        return termFactory.makeAppl(termFactory.makeConstructor("None", 0));
    }

    public String generateLex(Symbol symbol) {
        if (symbol instanceof CharacterClass) {
            return generateLex(((CharacterClass) symbol).symbol());
        } else if (symbol instanceof CharacterClassConc) {
            return generateCharacterClassConc((CharacterClassConc) symbol);
        } else if (symbol instanceof CharacterClassRange) {
            return generateCharacterClassRange((CharacterClassRange) symbol);
        } else if (symbol instanceof CharacterClassNumeric) {
            return generateCharacterClassNumeric((CharacterClassNumeric) symbol);
        } else if (symbol instanceof LexicalSymbol || symbol instanceof Sort) {
            return generateLexicalSymbol(symbol);
        }

        throw new IllegalStateException("Unknown symbol: " + symbol);
    }

    // TODO: toPrintableCharacter can and should be done in preprocessing. It takes precious time when done at runtime.
    // TODO: Right now, it just generates from the head of CharacterClassConc, it ignores characters in the tail
    public String generateCharacterClassConc(CharacterClassConc characterClassConc) {
        Symbol printableCharacters = Utils.toPrintable(characterClassConc);

        if (printableCharacters instanceof CharacterClassNumeric) {
            return generateCharacterClassNumeric((CharacterClassNumeric) printableCharacters);
        } else if (printableCharacters instanceof CharacterClassRange) {
            return generateCharacterClassRange((CharacterClassRange) printableCharacters);
        } else if (printableCharacters instanceof CharacterClassConc) {
            Symbol first = ((CharacterClassConc) printableCharacters).first();

            if (first instanceof CharacterClassRange) {
                CharacterClassRange range = (CharacterClassRange) first;

                return generateCharacterClassRange(range);
            } else if (first instanceof CharacterClassNumeric) {
                CharacterClassNumeric numeric = (CharacterClassNumeric) first;

                return generateCharacterClassNumeric(numeric);
            }
        }

        throw new IllegalStateException("Unknown symbol: " + printableCharacters);
    }

    /**
     * Generate a string consisting of a printable character from the given character range.
     *
     * @param characterClassRange
     * @return
     */
    public String generateCharacterClassRange(CharacterClassRange characterClassRange) {
        int minimumPrintable = Math.max(characterClassRange.minimum(), MINIMUM_PRINTABLE);
        int maximumPrintable = Math.min(characterClassRange.maximum(), MAXIMUM_PRINTABLE);

        int range = maximumPrintable - minimumPrintable + 1;

        if (range > 0) {
            char character = (char) (minimumPrintable + random.nextInt(range));

            return String.valueOf(character);
        } else {
            return "";
        }
    }

    // TODO: String concatenation is slow?
    public String generateCharacterClassNumeric(CharacterClassNumeric characterClassNumeric) {
        return String.valueOf(Character.toChars(characterClassNumeric.getCharacter()));
    }

    public String generateLexicalSymbol(Symbol symbol) {
        List<IProduction> productions = productionsMap.get(symbol);

        if (productions.isEmpty()) {
            throw new IllegalStateException("No productions found for symbol " + symbol);
        }

        IProduction production = random(productions);

        return production
                .rightHand()
                .stream()
                .map(this::generateLex)
                .collect(Collectors.joining());
    }

    public Optional<IStrategoTerm> generateCf(Symbol symbol, int size) {
        List<IProduction> productions = productionsMap.get(symbol);

        if (productions.isEmpty()) {
            return Optional.empty();
        }

        for (IProduction production : Utils.shuffle(productions)) {
            List<Symbol> rhsSymbols = cleanRhs(production.rightHand());
            int childSize = (size - 1) / Math.max(1, rhsSymbols.size());
            List<IStrategoTerm> children = new ArrayList<>();

            for (Symbol rhsSymbol : rhsSymbols) {
                Optional<IStrategoTerm> childTerm = generateSymbol(rhsSymbol, childSize);

                if (childTerm.isPresent()) {
                    children.add(childTerm.get());
                } else {
                    break;
                }
            }

            if (children.size() == rhsSymbols.size()) {
                Optional<String> constructor = getConstructor(production);

                if (constructor.isPresent()) {
                    return Optional.of(makeAppl(constructor.get(), children));
                } else {
                    return Optional.of(children.get(0));
                }
            }
        }

        return Optional.empty();
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
        return rightHand
                .stream()
                .filter(this::isProperSymbol)
                .collect(Collectors.toList());
    }

    protected boolean isProperSymbol(Symbol symbol) {
        return !"LAYOUT?-CF".equals(symbol.name()) && (
                symbol instanceof ContextFreeSymbol
                        || symbol instanceof FileStartSymbol
                        || symbol instanceof StartSymbol
                        || symbol instanceof LexicalSymbol
        );
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
        if (attribute instanceof GeneralAttribute) {
            GeneralAttribute generalAttribute = (GeneralAttribute) attribute;

            if (name.equals(generalAttribute.getName())) {
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
