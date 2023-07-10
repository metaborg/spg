package org.metaborg.spg.sentence.antlr;

import org.junit.Test;
import org.metaborg.spg.sentence.antlr.grammar.*;
import org.metaborg.util.collection.CapsuleUtil;

import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GrammarTest {
    @Test
    public void testInjections() {
        /*
         * conditionalOrExpression:
         *   conditionalAndExpression
         * | conditionalOrExpression '||" conditionalAndExpression;
         *
         * conditionalAndExpression:
         *   '42';
         */

        Grammar grammar = new Grammar("TestGrammar", Arrays.asList(
                new Rule("conditionalOrExpression", new Alt(
                        new Nonterminal("conditionalAndExpression"),
                        new Conc(
                                new Conc(
                                        new Nonterminal("conditionalOrExpression"),
                                        new Literal("||")
                                ),
                                new Nonterminal("conditionalAndExpression")
                        )
                )),

                new Rule("conditionalAndExpression", new Literal("42"))
        ));

        Set<Nonterminal> injections = grammar
                .getInjections(new Nonterminal("conditionalOrExpression"));

        Set<Nonterminal> expected = CapsuleUtil.toSet(
                new Nonterminal("conditionalOrExpression"),
                new Nonterminal("conditionalAndExpression")
        );

        assertThat(injections, is(expected));
    }

    @Test
    public void testInjectionsClosure() {
        /*
         * conditionalOrExpression:
         *   conditionalAndExpression
         * | conditionalOrExpression '||" conditionalAndExpression;
         *
         * conditionalAndExpression:
         *   inclusiveOrExpression
         * | conditionalAndExpression '&&' inclusiveOrExpression;
         *
         * inclusiveOrExpression:
         *   '42';
         */

        Grammar grammar = new Grammar("TestGrammar", Arrays.asList(
                new Rule("conditionalOrExpression", new Alt(
                        new Nonterminal("conditionalAndExpression"),
                        new Conc(
                                new Conc(
                                        new Nonterminal("conditionalOrExpression"),
                                        new Literal("||")
                                ),
                                new Nonterminal("conditionalAndExpression")
                        )
                )),

                new Rule("conditionalAndExpression", new Alt(
                        new Nonterminal("inclusiveOrExpression"),
                        new Conc(
                                new Conc(
                                        new Nonterminal("conditionalAndExpression"),
                                        new Literal("&&")
                                ),
                                new Nonterminal("inclusiveOrExpression")
                        )
                )),

                new Rule("inclusiveOrExpression", new Literal("42"))
        ));

        Set<Nonterminal> injectionsClosure = grammar
                .getInjections(new Nonterminal("conditionalOrExpression"));

        Set<Nonterminal> expected = CapsuleUtil.toSet(
                new Nonterminal("conditionalOrExpression"),
                new Nonterminal("conditionalAndExpression"),
                new Nonterminal("inclusiveOrExpression")
        );

        assertThat(injectionsClosure, is(expected));
    }
}
