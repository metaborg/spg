package org.metaborg.spg.sentence.antlr.grammar;

import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.*;

public class Grammar {
    private final String name;
    private final List<Rule> rules;
    private final Map<String, Rule> ruleCache;

    public Grammar(String name, List<Rule> rules) {
        this.name = name;
        this.rules = rules;
        this.ruleCache = Maps.uniqueIndex(rules, Rule::getName);
    }

    public String getName() {
        return name;
    }

    public Iterable<Rule> getRules() {
        return rules;
    }

    public Rule getRule(String name) {
        return ruleCache.get(name);
    }

    public Set<Nonterminal> getNonterminals() {
        return rules
                .stream()
                .flatMap(Rule::getNonterminals)
                .collect(toSet());
    }

    public int size() {
        return rules.size();
    }

    public Stream<Nonterminal> injectionsClosure(Nonterminal nonterminal) {
        return injectionsClosure(Collections.singleton(nonterminal));
    }

    public Stream<Nonterminal> injectionsClosure(Set<Nonterminal> nonterminals) {
        Stream<Nonterminal> injections = nonterminals.stream().flatMap(n ->
                concat(of(n), injections(n))
        );

        Set<Nonterminal> ns = injections.collect(toSet());

        if (!ns.equals(nonterminals)) {
            return injectionsClosure(ns);
        } else {
            return nonterminals.stream();
        }
    }

    public Stream<Nonterminal> injections(Nonterminal n) {
        Rule rule = getRule(n.getName());

        return injections(rule.getElement());
    }

    public Stream<Nonterminal> injections(ElementOpt elementOpt) {
        if (elementOpt instanceof Alt) {
            Alt alt = (Alt) elementOpt;

            return concat(injections(alt.getFirst()), injections(alt.getSecond()));
        } else if (elementOpt instanceof Nonterminal) {
            Nonterminal nonterminal = (Nonterminal) elementOpt;

            return of(nonterminal);
        }

        return empty();
    }
}
