package org.metaborg.spg.sentence.antlr.grammar;

import com.google.common.collect.Iterables;

public class Grammar {
    private final Iterable<Rule> rules;

    public Grammar(String name, Iterable<Rule> rules) {
        this.rules = rules;
    }

    public Rule getInitialRule() {
        return Iterables.getFirst(rules, null);
    }

    public Nonterminal getStart() {
        Rule initialRule = getInitialRule();

        return new Nonterminal(initialRule.getName());
    }

    public Iterable<Rule> getRules() {
        return rules;
    }
}
