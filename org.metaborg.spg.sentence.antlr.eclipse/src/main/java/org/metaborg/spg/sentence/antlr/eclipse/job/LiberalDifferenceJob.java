package org.metaborg.spg.sentence.antlr.eclipse.job;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.antlr.v4.tool.Grammar;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spg.sentence.antlr.eclipse.Activator;
import org.metaborg.spg.sentence.antlr.eclipse.config.DifferenceJobConfig;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spg.sentence.sdf3.GrammarFactory;
import org.metaborg.spg.sentence.shrinker.Shrinker;
import org.metaborg.spg.sentence.shrinker.ShrinkerFactory;
import org.metaborg.spg.sentence.signature.Signature;
import org.metaborg.spg.sentence.signature.SignatureFactory;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.io.IOException;
import java.util.Optional;

import static org.metaborg.spg.sentence.shared.utils.SpoofaxUtils.loadLanguage;

public class LiberalDifferenceJob extends DifferenceJob {
    private static final JSGLRParserConfiguration PARSER_CONFIG = new JSGLRParserConfiguration(true, false, false, 30000, Integer.MAX_VALUE);
    // TODO: Make this part of the build (unpack to resources)
    private static final String TEMPLATE_LANG = "/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.4.0-SNAPSHOT.spoofax-language";

    private final PrinterFactory printerFactory;
    private final GeneratorFactory generatorFactory;
    private final GrammarFactory grammarFactory;
    private final SignatureFactory signatureFactory;
    private final ShrinkerFactory shrinkerFactory;
    private final DifferenceJobConfig config;
    private final ILanguageImpl templateLanguage;

    @Inject
    public LiberalDifferenceJob(
            ISpoofaxUnitService unitService,
            ISpoofaxSyntaxService syntaxService,
            PrinterFactory printerFactory,
            GeneratorFactory generatorFactory,
            GrammarFactory grammarFactory,
            SignatureFactory signatureFactory,
            ShrinkerFactory shrinkerFactory,
            @Assisted DifferenceJobConfig config) throws MetaborgException {
        super(unitService, syntaxService, "Difference test (liberal)");

        this.printerFactory = printerFactory;
        this.generatorFactory = generatorFactory;
        this.grammarFactory = grammarFactory;
        this.signatureFactory = signatureFactory;
        this.shrinkerFactory = shrinkerFactory;
        this.config = config;
        this.templateLanguage = loadLanguage(SpoofaxPlugin.spoofax(), TEMPLATE_LANG);
    }

    @Override
    protected IStatus run(IProgressMonitor progressMonitor) {
        IProject project = config.getProject();
        ILanguageImpl language = config.getLanguage();
        int maxNumberOfTerms = config.getMaxNumberOfTerms();
        int maxTermSize = config.getMaxTermSize();
        Grammar antlrGrammar = Grammar.load(config.getAntlrGrammar());
        String antlrStartSymbol = config.getAntlrStartSymbol();

        SubMonitor subMonitor = SubMonitor.convert(progressMonitor, maxNumberOfTerms);

        try {
            Printer printer = printerFactory.create(language, project);
            Generator generator = generatorFactory.create(language, project);
            org.metaborg.spg.sentence.sdf3.Grammar grammar = grammarFactory.create(templateLanguage, project);
            Signature signature = signatureFactory.create(grammar);
            Shrinker shrinker = shrinkerFactory.create(generator, signature);

            for (int i = 0; i < maxNumberOfTerms; i++) {
                try {
                    // Generate
                    Optional<IStrategoTerm> termOpt = generator.generate(maxTermSize);

                    if (termOpt.isPresent()) {
                        IStrategoTerm term = termOpt.get();
                        String text = printer.print(term);

                        stream.println("=== Program ===");
                        stream.println(text);

                        if (cannotParseAntlr(antlrGrammar, antlrStartSymbol, text)) {
                            if (canParseSpoofax(config.getLanguage(), text, PARSER_CONFIG)) {
                                stream.println("=== Legal SDF3 illegal ANTLRv4 sentence ===");
                                stream.println(text);

                                // Shrink
                                try {
                                    shrinkStar(subMonitor, shrinker, printer, antlrGrammar, antlrStartSymbol, language, term);
                                } catch (Exception e) {
                                    Activator.logError("An unexpected error occurred.", e);
                                }

                                break;
                            }
                        }
                    }

                    subMonitor.split(1);
                } catch (Exception e) {
                    Activator.logError("An unexpected error occurred.", e);
                }
            }

            return Status.OK_STATUS;
        } catch (Exception e) {
            return Status.CANCEL_STATUS;
        }
    }

    private IStrategoTerm shrinkStar(SubMonitor subMonitor, Shrinker shrinker, Printer printer, Grammar antlrGrammar, String antlrStartSymbol, ILanguageImpl language, IStrategoTerm term) throws IOException, ParseException {
        subMonitor.setWorkRemaining(50).split(1);

        Optional<IStrategoTerm> shrinkOpt = shrink(shrinker, printer, antlrGrammar, antlrStartSymbol, language, term);

        if (shrinkOpt.isPresent()) {
            return shrinkStar(subMonitor, shrinker, printer, antlrGrammar, antlrStartSymbol, language, shrinkOpt.get());
        } else {
            return term;
        }
    }

    private Optional<IStrategoTerm> shrink(Shrinker shrinker, Printer printer, Grammar antlrGrammar, String antlrStartSymbol, ILanguageImpl language, IStrategoTerm term) throws IOException, ParseException {
        Iterable<IStrategoTerm> iterable = shrinker.shrink(term)::iterator;

        for (IStrategoTerm shrunkTerm : iterable) {
            String shrunkText = printer.print(shrunkTerm);

            if (cannotParseAntlr(antlrGrammar, antlrStartSymbol, shrunkText)) {
                if (canParseSpoofax(language, shrunkText, PARSER_CONFIG)) {
                    stream.println("=== Shrunk to " + shrunkText.length() + " characters ===");
                    stream.println(shrunkText);

                    return Optional.of(shrunkTerm);
                }
            }
        }

        return Optional.empty();
    }
}
