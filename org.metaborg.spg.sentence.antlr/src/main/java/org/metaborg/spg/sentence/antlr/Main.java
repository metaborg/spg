package org.metaborg.spg.sentence.antlr;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spg.sentence.antlr.generator.Generator;
import org.metaborg.spg.sentence.antlr.generator.GeneratorFactory;
import org.metaborg.spg.sentence.antlr.grammar.Grammar;
import org.metaborg.spg.sentence.antlr.grammar.GrammarFactory;
import org.metaborg.spoofax.core.Spoofax;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

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

        try (final Spoofax spoofax = new Spoofax(new Module(0))) {
            FileObject languageLocation = spoofax.resourceService.resolve(antlrLanguageFile);
            ILanguageImpl antlrLanguageImpl = spoofax.languageDiscoveryService.languageFromArchive(languageLocation);

            GrammarFactory grammarFactory = spoofax.injector.getInstance(GrammarFactory.class);
            Grammar grammar = grammarFactory.create(grammarFile, antlrLanguageImpl);

            GeneratorFactory generatorFactory = spoofax.injector.getInstance(GeneratorFactory.class);
            Generator generator = generatorFactory.create(grammar);

            while (true) {
                Optional<String> sentenceOpt = generator.generate(10000000);

                if (sentenceOpt.isPresent()) {
                    System.out.println(sentenceOpt.get());
                }
            }
        } catch (MetaborgException | IOException e) {
            e.printStackTrace();
        }
    }
}
