package org.metaborg.spg.sentence;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.syntax.ParseException;
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

    @Inject
    public ShrinkerFactory(SignatureReaderFactory signatureReaderFactory, IResourceService resourceService) {
        this.signatureReaderFactory = signatureReaderFactory;
        this.resourceService = resourceService;
    }

    public Shrinker create(ILanguageImpl language, IProject project, Generator generator, ITermFactory termFactory, ILanguageImpl strategoLanguage) throws IOException, ParseException {
        SpoofaxCommonPaths spoofaxCommonPaths = new SpoofaxCommonPaths(project.location());

        FileObject mainSignatureFile = getMainSignatureFile(spoofaxCommonPaths, language.id().id);
        FileObject includePath = spoofaxCommonPaths.syntaxSrcGenSignatureDir();

        SignatureReader signatureReader = signatureReaderFactory.create(strategoLanguage);
        Signature signature = signatureReader.read(mainSignatureFile, includePath);

        String rootSort = getRootSort(language);

        return new Shrinker(generator, termFactory, signature, rootSort);
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
