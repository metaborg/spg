package org.metaborg.spg.sentence.antlr.eclipse.job;

import static org.metaborg.spg.sentence.shared.utils.FunctionalUtils.uncheckPredicate;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spg.sentence.antlr.eclipse.config.DifferenceJobConfig;
import org.metaborg.spg.sentence.antlr.generator.Generator;
import org.metaborg.spg.sentence.antlr.generator.GeneratorFactory;
import org.metaborg.spg.sentence.antlr.grammar.Grammar;
import org.metaborg.spg.sentence.antlr.grammar.GrammarFactory;
import org.metaborg.spg.sentence.antlr.shrinker.Shrinker;
import org.metaborg.spg.sentence.antlr.shrinker.ShrinkerFactory;
import org.metaborg.spg.sentence.antlr.term.Term;
import org.metaborg.spg.sentence.shared.utils.SpoofaxUtils;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class RestrictiveDifferenceJob extends DifferenceJob {
    private static final JSGLRParserConfiguration PARSER_CONFIG = new JSGLRParserConfiguration(false, false);

    private static final String ANTLR_LANG_NAME = "ANTLRv4";

    private final GrammarFactory grammarFactory;
    private final GeneratorFactory generatorFactory;
    private final ShrinkerFactory shrinkerFactory;
    private final DifferenceJobConfig config;

    @jakarta.inject.Inject @javax.inject.Inject
    public RestrictiveDifferenceJob(
            ISpoofaxUnitService unitService,
            ISpoofaxSyntaxService syntaxService,
            GrammarFactory grammarFactory,
            GeneratorFactory generatorFactory,
            ShrinkerFactory shrinkerFactory,
            @Assisted DifferenceJobConfig config) {
        super(unitService, syntaxService, "Difference test (restrictive)");

        this.grammarFactory = grammarFactory;
        this.generatorFactory = generatorFactory;
        this.shrinkerFactory = shrinkerFactory;
        this.config = config;
    }

    @Override
    protected IStatus run(IProgressMonitor progressMonitor) {
        try {
            ILanguageImpl language = config.getLanguage();
            String antlrStartSymbol = config.getAntlrStartSymbol();
            int maxTermSize = config.getMaxTermSize();
            int maxNumberOfTerms = config.getMaxNumberOfTerms();
            ILanguageImpl antlrLanguageImpl = SpoofaxUtils.getLanguage(spoofax, ANTLR_LANG_NAME);
            org.antlr.v4.tool.Grammar antlrGrammar = org.antlr.v4.tool.Grammar.load(config.getAntlrGrammar());
            SubMonitor subMonitor = SubMonitor.convert(progressMonitor, maxNumberOfTerms);

            Grammar grammar = grammarFactory.create(spoofax.resolve(config.getAntlrGrammar()), antlrLanguageImpl);
            Generator generator = generatorFactory.create(grammar);
            Shrinker shrinker = shrinkerFactory.create(generator, grammar);

            for (int i = 0; i < maxNumberOfTerms; i++) {
                subMonitor.split(1);

                // Generate
                Optional<Term> treeOpt = generator.generate(antlrStartSymbol, maxTermSize);

                if (treeOpt.isPresent()) {
                    Term term = treeOpt.get();
                    String sentence = term.toString(true);

                    stream.println("=== Program ===");
                    stream.println(sentence);

                    boolean spoofaxResult = canParseSpoofax(language, sentence, PARSER_CONFIG);

                    if (!spoofaxResult) {
                        boolean antlrResult = canParseAntlr(antlrGrammar, antlrStartSymbol, sentence);

                        if (antlrResult) {
                            stream.println("=== Legal ANTLRv4 illegal SDF3 sentence ===");
                            stream.println(sentence);

                            // Shrink
                            while (true) {
                                subMonitor.setWorkRemaining(50).split(1);

                                Stream<Term> shrunkTrees = shrinker.shrink(term);

                                Optional<Term> anyShrunkTree = shrunkTrees
                                        .filter(uncheckPredicate(shrunkTree -> !canParseSpoofax(language, shrunkTree.toString(), PARSER_CONFIG)))
                                        .filter(uncheckPredicate(shrunkTree -> canParseAntlr(antlrGrammar, antlrStartSymbol, shrunkTree.toString())))
                                        .findFirst();

                                if (anyShrunkTree.isPresent()) {
                                    String shrunkText = anyShrunkTree.get().toString();

                                    stream.println("=== Shrunk to " + shrunkText.length() + " characters ===");
                                    stream.println(shrunkText);

                                    term = anyShrunkTree.get();
                                } else {
                                    break;
                                }
                            }

                            break;
                        }
                    }
                }
            }
        } catch (IOException | MetaborgException e) {
            e.printStackTrace();
        }

        return Status.OK_STATUS;
    }
}
