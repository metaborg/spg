package org.metaborg.spg.sentence.antlr;

import org.antlr.v4.runtime.*;
import org.antlr.v4.tool.Rule;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spg.sentence.antlr.generator.Generator;
import org.metaborg.spg.sentence.antlr.generator.GeneratorFactory;
import org.metaborg.spg.sentence.antlr.grammar.Grammar;
import org.metaborg.spg.sentence.antlr.grammar.GrammarFactory;
import org.metaborg.spg.sentence.antlr.shrinker.Shrinker;
import org.metaborg.spg.sentence.antlr.shrinker.ShrinkerFactory;
import org.metaborg.spg.sentence.antlr.shrinker.ShrinkerIterable;
import org.metaborg.spg.sentence.antlr.shrinker.ShrinkerIterator;
import org.metaborg.spg.sentence.antlr.term.Term;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Stream;

import static org.metaborg.spg.sentence.antlr.functional.Utils.uncheck;

public class Main {
    public static void main(String[] args) {
        File antlrLanguageFile = new File(args[0]);

        if (!antlrLanguageFile.exists()) {
            System.err.println("The provided ANTLRv4 .spoofax-language file does not exist.");

            return;
        }

        File grammarFile = new File(args[1]);

        if (!grammarFile.exists()) {
            System.err.println("The provided ANTLRv4 grammar file does not exist.");

            return;
        }
        
        File minijavaLanguageFile = new File(args[2]);

        if (!minijavaLanguageFile.exists()) {
            System.err.println("The provided MiniJava language file does not exist.");

            return;
        }

        String antlrStartSymbol = args[3];

        int maxSize = Integer.valueOf(args[4]);

        try (final Spoofax spoofax = new Spoofax(new Module(0))) {
            ILanguageImpl antlrLanguageImpl = getLanguageImpl(spoofax, antlrLanguageFile);
            ILanguageImpl minijavaLanguageImpl = getLanguageImpl(spoofax, minijavaLanguageFile);

            GrammarFactory grammarFactory = spoofax.injector.getInstance(GrammarFactory.class);
            Grammar grammar = grammarFactory.create(grammarFile, antlrLanguageImpl);

            GeneratorFactory generatorFactory = spoofax.injector.getInstance(GeneratorFactory.class);
            Generator generator = generatorFactory.create(grammar);

            ShrinkerFactory shrinkerFactory = spoofax.injector.getInstance(ShrinkerFactory.class);
            Shrinker shrinker = shrinkerFactory.create(generator);

            org.antlr.v4.tool.Grammar antlrGrammar = org.antlr.v4.tool.Grammar.load(args[1]);

            for (int i = 0; i < 10000; i++) {
                Optional<Term> treeOpt = generator.generate(antlrStartSymbol, maxSize);

                if (treeOpt.isPresent()) {
                    Term term = treeOpt.get();
                    String sentence = term.toString(true);

                    System.out.println(sentence);

                    boolean antlrResult = parse(antlrGrammar, antlrStartSymbol, sentence);

                    if (antlrResult) {
                        ISpoofaxParseUnit parseUnit = parse(spoofax, minijavaLanguageImpl, sentence);

                        if (!parseUnit.success()) {
                            System.out.println("Legal ANTLRv4 illegal SDF3 sentence:");
                            System.out.println(sentence);
                            
                            while (true) {
                                Stream<Term> shrunkTrees = shrinker.shrink(term);

                                Optional<Term> anyShrunkTree = shrunkTrees.filter(uncheck(shrunkTree ->
                                        parse(antlrGrammar, antlrStartSymbol, shrunkTree.toString(true))
                                )).filter(uncheck(shrunkTree ->
                                        !parse(spoofax, minijavaLanguageImpl, shrunkTree.toString(true)).success()
                                )).findAny();

                                if (anyShrunkTree.isPresent()) {
                                    System.out.println("Shrunk sentence to smaller sentence:");
                                    System.out.println(anyShrunkTree.get().toString(true));

                                    term = anyShrunkTree.get();
                                } else {
                                    break;
                                }
                            }
                        }
                    } else {
                        System.err.println("Unparsable sentence: " + sentence);
                    }
                }
            }
        } catch (MetaborgException | IOException e) {
            e.printStackTrace();
        }
    }

    private static ILanguageImpl getLanguageImpl(Spoofax spoofax, File antlrLanguageFile) throws MetaborgException {
        FileObject languageLocation = spoofax.resourceService.resolve(antlrLanguageFile);

        return spoofax.languageDiscoveryService.languageFromArchive(languageLocation);
    }

    private static boolean parse(org.antlr.v4.tool.Grammar grammar, String antlrStartSymbol, String text) throws IOException {
        CharStream charStream = CharStreams.fromString(text);
        LexerInterpreter lexer = grammar.createLexerInterpreter(charStream);
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ParserInterpreter parser = grammar.createParserInterpreter(tokens);

        Rule startRule = grammar.getRule(antlrStartSymbol);

        // Executed for its side-effect
        parser.parse(startRule.index);

        return parser.getNumberOfSyntaxErrors() == 0;
    }

    private static ISpoofaxParseUnit parse(Spoofax spoofax, ILanguageImpl languageImpl, String text) throws ParseException {
        ISpoofaxInputUnit inputUnit = spoofax.unitService.inputUnit(text, languageImpl, null);

        return spoofax.syntaxService.parse(inputUnit);
    }
}
