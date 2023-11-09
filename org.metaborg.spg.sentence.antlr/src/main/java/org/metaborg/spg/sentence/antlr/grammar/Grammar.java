package org.metaborg.spg.sentence.antlr.grammar;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.*;
import static org.metaborg.spg.sentence.shared.utils.StreamUtils.cons;

public class Grammar {
    private final String name;
    private final List<Rule> rules;
    private final Map<String, Rule> ruleCache;
    private final Map<Nonterminal, Set<Nonterminal>> injectionCache;

    public Grammar(String name, List<Rule> rules) {
        this.name = name;
        this.rules = rules;
        this.ruleCache = rules.stream().collect(Collectors.toMap(Rule::getName, Function.identity()));
        this.injectionCache = getNonterminals().collect(Collectors.toMap(Function.identity(), this::injectionsClosure));
    }

    public String getName() {
        return name;
    }

    public Iterable<Rule> getRules() {
        return rules;
    }

    public Rule getRule(String name) {
        Rule rule = ruleCache.get(name);

        if (rule == null) {
            throw new IllegalArgumentException("No rule with name " + name);
        }

        return ruleCache.get(name);
    }

    public Stream<Nonterminal> getNonterminals() {
        return rules
                .stream()
                .map(Rule::getName)
                .map(Nonterminal::new);
    }

    public Map<Nonterminal, Set<Nonterminal>> getInjections() {
        return injectionCache;
    }

    public Set<Nonterminal> getInjections(Nonterminal nonterminal) {
        return injectionCache.get(nonterminal);
    }

    public int size() {
        return rules.size();
    }

    private Set<Nonterminal> injectionsClosure(Nonterminal nonterminal) {
        return injectionsClosure(Collections.singleton(nonterminal));
    }

    private Set<Nonterminal> injectionsClosure(Set<Nonterminal> nonterminals) {
        Stream<Nonterminal> injections = nonterminals.stream().flatMap(n ->
                cons(n, injections(n))
        );

        Set<Nonterminal> ns = injections.collect(toSet());

        if (!ns.equals(nonterminals)) {
            return injectionsClosure(ns);
        } else {
            return nonterminals;
        }
    }

    private Stream<Nonterminal> injections(Nonterminal n) {
        Rule rule = getRule(n.getName());

        return injections(rule.getEmptyElement());
    }

    private Stream<Nonterminal> injections(EmptyElement emptyElement) {
        if (emptyElement instanceof Alt) {
            Alt alt = (Alt) emptyElement;

            return concat(injections(alt.getFirst()), injections(alt.getSecond()));
        } else if (emptyElement instanceof Nonterminal) {
            Nonterminal nonterminal = (Nonterminal) emptyElement;

            return of(nonterminal);
        }

        return empty();
    }
}
