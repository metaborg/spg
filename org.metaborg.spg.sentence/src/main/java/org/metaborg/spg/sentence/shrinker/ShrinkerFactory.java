package org.metaborg.spg.sentence.shrinker;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spg.sentence.ParseService;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.signature.Signature;
import org.metaborg.spg.sentence.signature.SignatureReader;
import org.metaborg.spg.sentence.signature.SignatureReaderFactory;
import org.metaborg.spoofax.core.build.SpoofaxCommonPaths;
import org.metaborg.spoofax.core.syntax.SyntaxFacet;
import org.spoofax.interpreter.terms.ITermFactory;

import java.io.IOException;

public class ShrinkerFactory {
    private final SignatureReaderFactory signatureReaderFactory;
    private IResourceService resourceService;
    private final ParseService parseService;

    @Inject
    public ShrinkerFactory(SignatureReaderFactory signatureReaderFactory, IResourceService resourceService, ParseService parseService) {
        this.signatureReaderFactory = signatureReaderFactory;
        this.resourceService = resourceService;
        this.parseService = parseService;
    }

    // TODO: Reduce number of arguments.
    public Shrinker create(ILanguageImpl language, IProject project, Printer printer, Generator generator, ITermFactory termFactory, ILanguageImpl strategoLanguage) throws IOException, ParseException {
        SpoofaxCommonPaths spoofaxCommonPaths = new SpoofaxCommonPaths(project.location());

        FileObject mainSignatureFile = getMainSignatureFile(spoofaxCommonPaths, language.id().id);
        FileObject includePath = spoofaxCommonPaths.syntaxSrcGenSignatureDir();

        SignatureReader signatureReader = signatureReaderFactory.create(strategoLanguage);
        Signature signature = signatureReader.read(mainSignatureFile, includePath);

        String rootSort = getRootSort(language);
        ShrinkerConfig shrinkerConfig = new ShrinkerConfig(language, signature, rootSort, printer);

        return new Shrinker(parseService, generator, termFactory, shrinkerConfig);
    }

    // TODO: Move to SpoofaxCommonPaths in org.metaborg.spoofax.core
    protected FileObject getMainSignatureFile(SpoofaxCommonPaths spoofaxCommonPaths, String languageName) {
        FileObject signatureDir = spoofaxCommonPaths.syntaxSrcGenSignatureDir();

        return resourceService.resolve(signatureDir, languageName + "-sig.str");
    }

    protected String getRootSort(ILanguageImpl language) {
        SyntaxFacet syntaxFacet = language.facet(SyntaxFacet.class);

        if (syntaxFacet == null) {
            throw new IllegalStateException("Unable to get syntax facet.");
        }

        return Iterables.get(syntaxFacet.startSymbols, 0);
    }
}
