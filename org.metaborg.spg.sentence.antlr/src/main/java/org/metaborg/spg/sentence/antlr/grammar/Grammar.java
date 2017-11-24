package org.metaborg.spg.sentence.antlr.grammar;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Grammar {
    private final Iterable<Rule> rules;

    public Grammar(String name, Iterable<Rule> rules) {
        this.rules = rules;
    }

    private static <T> Stream<T> iterableToStream(final Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public Nonterminal getStart() {
        Optional<Rule> ruleOpt = getRule("compilationUnit");

        if (!ruleOpt.isPresent()) {
            throw new IllegalStateException("Start rule not found.");
        }

        return new Nonterminal(ruleOpt.get().getName());
    }

    public Iterable<Rule> getRules() {
        return rules;
    }

    public Optional<Rule> getRule(String name) {
        return iterableToStream(rules)
                .filter(rule -> "compilationUnit".equals(rule.getName()))
                .findFirst();
    }
}
