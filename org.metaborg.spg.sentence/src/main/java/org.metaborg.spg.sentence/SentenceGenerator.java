package org.metaborg.spg.sentence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.metaborg.core.project.IProject;
import org.metaborg.sdf2table.grammar.CharacterClass;
import org.metaborg.sdf2table.grammar.ConstructorAttribute;
import org.metaborg.sdf2table.grammar.ContextFreeSymbol;
import org.metaborg.sdf2table.grammar.FileStartSymbol;
import org.metaborg.sdf2table.grammar.GeneralAttribute;
import org.metaborg.sdf2table.grammar.IAttribute;
import org.metaborg.sdf2table.grammar.IProduction;
import org.metaborg.sdf2table.grammar.LexicalSymbol;
import org.metaborg.sdf2table.grammar.NormGrammar;
import org.metaborg.sdf2table.grammar.Sort;
import org.metaborg.sdf2table.grammar.StartSymbol;
import org.metaborg.sdf2table.grammar.Symbol;
import org.metaborg.sdf2table.io.GrammarReader;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.StrategoConstructor;
import org.spoofax.terms.TermFactory;

public class SentenceGenerator {
  private final IProject project;
  private ListMultimap<Symbol, IProduction> productionMap;
  private NormGrammar grammar;
  private java.util.Random random = new java.util.Random();
  private TermFactory termFactory = new TermFactory();

  public SentenceGenerator(IProject project) throws Exception {
    this.project = project;

    GrammarReader grammarReader = new GrammarReader(termFactory);
    
    // TODO: Take project as input, dynamically discover this file
    File input = new File("/tmp/metaborg-scopes-frames/L1/src-gen/syntax/normalized/L1-norm.aterm");
    List<String> paths = Collections.singletonList("/tmp/metaborg-scopes-frames/L1/src-gen/syntax");
    grammar = grammarReader.readGrammar(input, paths);
    Collection<IProduction> productions = grammar.getCacheProductionsRead().values();
    Collection<IProduction> realProductions = retainRealProductions(productions);

    productionMap = createProductionMap(realProductions);
  }

  public Optional<IStrategoTerm> generate() throws Exception {
    // TODO: Do not use initial production; use the start of the grammar (see Scala implementation)

    IProduction initialProduction = grammar.getInitialProduction();

    return generate(initialProduction.leftHand(), 100);
  }

  public Optional<IStrategoTerm> generate(Symbol symbol, int size) {
    if (symbol.name().endsWith("-LEX")) {
      return Optional.of(termFactory.makeString(generateLex(symbol)));
    } else {
      return generateCf(symbol, size);
    }
  }

  public String generateLex(Symbol symbol) {
    if (symbol instanceof CharacterClass) {
      return generateCharacterClass((CharacterClass) symbol);
    } else if (symbol instanceof LexicalSymbol || symbol instanceof Sort) {
      return generateLexicalSymbol(symbol);
    }

    throw new IllegalStateException("Unknown symbol: " + symbol);
  }

  public String generateCharacterClass(CharacterClass characterClass) {
    int range = characterClass.maximum() - characterClass.minimum() + 1;
    char character = (char) (characterClass.minimum() + random.nextInt(range));

    return String.valueOf(character);
  }

  public String generateLexicalSymbol(Symbol symbol) {
    List<IProduction> productions = productionMap.get(symbol);
    IProduction production = random(productions);

    return production.rightHand()
        .stream()
        .map(this::generateLex)
        .collect(Collectors.joining());
  }

  public Optional<IStrategoTerm> generateCf(Symbol symbol, int size) {
    if (size <= 0) {
      return Optional.empty();
    }

    List<IProduction> productions = productionMap.get(symbol);

    if (productions.isEmpty()) {
      return Optional.empty();
    }

    IProduction production = random(productions);
    List<Symbol> symbols = cleanRhs(production.rightHand());
    int childSize = (size - 1) / Math.max(1, symbols.size());
    List<IStrategoTerm> children = new ArrayList<>();

    for (Symbol rhsSymbol : symbols) {
      Optional<IStrategoTerm> childTerm = generate(rhsSymbol, childSize);

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
   * Create a term based on the given child terms.
   *
   * @param children
   * @return
   */
  protected IStrategoTerm makeString(List<IStrategoTerm> children) {
    StringBuilder stringBuilder = new StringBuilder();

    for (IStrategoTerm child : children) {
      IStrategoString childString = (IStrategoString) child;

      stringBuilder.append(childString.stringValue());
    }

    return termFactory.makeString(stringBuilder.toString());
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
   * Check if the given production is a placeholder production.
   *
   * @param production
   * @return
   */
  protected boolean isPlaceholder(IProduction production) {
    Optional<IAttribute> findAttribute = findAttribute(production, this::isPlaceholderAttribute);

    return findAttribute.isPresent();
  }

  /**
   * Check if the given attribute is a placeholder attribute.
   *
   * @param attribute
   * @return
   */
  protected boolean isPlaceholderAttribute(IAttribute attribute) {
    if (attribute instanceof GeneralAttribute) {
      GeneralAttribute generalAttribute = (GeneralAttribute) attribute;

      if ("placeholder".equals(generalAttribute.getName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Check if the given production is a recover production.
   *
   * @param production
   * @return
   */
  protected boolean isRecover(IProduction production) {
    Optional<IAttribute> findAttribute = findAttribute(production, this::isRecoverAttribute);

    return findAttribute.isPresent();
  }

  /**
   * Check if the given attribute is a recover attribute.
   *
   * @param attribute
   * @return
   */
  protected boolean isRecoverAttribute(IAttribute attribute) {
    if (attribute instanceof GeneralAttribute) {
      GeneralAttribute generalAttribute = (GeneralAttribute) attribute;

      if ("recover".equals(generalAttribute.getName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Find the first attribute on the given production that satisfies the given predicate.
   *
   * @param production
   * @param predicate
   * @return
   */
  protected Optional<IAttribute> findAttribute(IProduction production, Predicate<IAttribute> predicate) {
    Set<IAttribute> attributes = grammar.getProductionAttributesMapping().get(production);

    for (IAttribute attribute : attributes) {
      if (predicate.test(attribute)) {
        return Optional.of(attribute);
      }
    }

    return Optional.empty();
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
        .filter(symbol -> !"LAYOUT?-CF".equals(symbol.name()) && (symbol instanceof ContextFreeSymbol || symbol instanceof FileStartSymbol || symbol instanceof StartSymbol || symbol instanceof LexicalSymbol))
        .collect(Collectors.toList());
  }

  /**
   * Create a multimap from left-hand symbol to production.
   *
   * @param productions
   * @return
   */
  protected ListMultimap<Symbol, IProduction> createProductionMap(Collection<IProduction> productions) {
    ListMultimap<Symbol, IProduction> productionMap = ArrayListMultimap.create();

    for (IProduction production : productions) {
      productionMap.put(production.leftHand(), production);
    }

    return productionMap;
  }

  /**
   * Remove placeholder productions from the given collection of productions.
   *
   * @param productions
   * @return
   */
  protected Collection<IProduction> retainRealProductions(Collection<IProduction> productions) {
    return productions.stream().filter(this::isRealProduction).collect(Collectors.toList());
  }

  /**
   * Check that this is neither a placeholder nor a recover production.
   *
   * @param production
   * @return
   */
  protected boolean isRealProduction(IProduction production) {
    return !isPlaceholder(production) && !isRecover(production);
  }
}
