package org.metaborg.spg.sentence.generator;

import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.sdf2table.grammar.NormGrammar;
import org.metaborg.sdf2table.io.NormGrammarReader;
import org.metaborg.spg.sentence.random.IRandom;
import org.metaborg.spg.sentence.terms.GeneratorTermFactory;
import org.metaborg.spoofax.core.build.SpoofaxCommonPaths;
import org.metaborg.spoofax.core.syntax.SyntaxFacet;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GeneratorFactory {
    private final IResourceService resourceService;
    private final GeneratorTermFactory termFactory;
    private final IRandom random;

    @jakarta.inject.Inject
    public GeneratorFactory(IResourceService resourceService, GeneratorTermFactory termFactory, IRandom random) {
        this.resourceService = resourceService;
        this.termFactory = termFactory;
        this.random = random;
    }

    public Generator create(ILanguageImpl language, IProject project) throws Exception {
        SpoofaxCommonPaths spoofaxCommonPaths = new SpoofaxCommonPaths(project.location());

        File syntaxMainFile = getSyntaxMainFile(spoofaxCommonPaths, language);
        List<String> syntaxPath = getSyntaxPath(spoofaxCommonPaths);
        
        NormGrammarReader grammarReader = new NormGrammarReader(syntaxPath);
        NormGrammar grammar = grammarReader.readGrammar(syntaxMainFile);

        String startSymbol = getStartSymbol(language);

        return new Generator(termFactory, random, startSymbol, grammar);
    }

    protected File getSyntaxMainFile(SpoofaxCommonPaths spoofaxCommonPaths, ILanguageImpl language) {
        return resourceService.localFile(spoofaxCommonPaths.syntaxSrcGenMainNormFile(language.belongsTo().name()));
    }

    protected File getSyntaxDirectory(SpoofaxCommonPaths spoofaxCommonPaths) {
        return resourceService.localFile(spoofaxCommonPaths.syntaxSrcGenDir());
    }

    protected List<String> getSyntaxPath(SpoofaxCommonPaths spoofaxCommonPaths) {
        File syntaxDirectory = getSyntaxDirectory(spoofaxCommonPaths);

        return Collections.singletonList(syntaxDirectory.getAbsolutePath());
    }

    public String getStartSymbol(ILanguageImpl languageImpl) {
        SyntaxFacet syntaxFacet = languageImpl.facet(SyntaxFacet.class);

        if (syntaxFacet == null) {
            return null;
        }

        final Iterator<String> it = syntaxFacet.startSymbols.iterator();
        return it.hasNext() ? it.next() : null;
    }
}
