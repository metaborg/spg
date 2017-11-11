package org.metaborg.spg.sentence.signature;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.sdf2table.grammar.Sort;
import org.metaborg.spg.sentence.ParseService;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SignatureReader {
    private ParseService parseService;
    private ISourceTextService sourceTextService;
    private ILanguageImpl strategoLanguageImpl;

    @Inject
    public SignatureReader(ParseService parseService, ISourceTextService sourceTextService, @Assisted ILanguageImpl strategoLanguageImpl) {
        this.parseService = parseService;
        this.sourceTextService = sourceTextService;
        this.strategoLanguageImpl = strategoLanguageImpl;
    }

    public Signature read(FileObject mainSignatureFile, FileObject... paths) throws IOException, ParseException {
        return read(mainSignatureFile, Arrays.asList(paths));
    }

    public Signature read(FileObject mainSignatureFile, Collection<FileObject> path) throws IOException, ParseException {
        // TODO: Follow imports to get a complete view

        return read(mainSignatureFile);
    }

    protected Signature read(FileObject signatureFile) throws IOException, ParseException {
        String text = sourceTextService.text(signatureFile);
        IStrategoTerm term = parseService.parse(strategoLanguageImpl, text);

        return readModule(term);
    }

    protected Signature readModule(IStrategoTerm term) {
        if (term instanceof IStrategoAppl) {
            IStrategoAppl appl = (IStrategoAppl) term;

            if (appl.getName().equals("Module")) {
                IStrategoList list = (IStrategoList) appl.getSubterm(1);
                IStrategoTerm signatureTerm = list.getSubterm(1);

                return readSignature(signatureTerm);
            }
        }

        throw new IllegalArgumentException("Could not read term as module.");
    }

    protected Signature readSignature(IStrategoTerm term) {
        IStrategoList constructorList = (IStrategoList) term.getSubterm(0).getSubterm(0).getSubterm(0);

        List<Constructor> constructors = Arrays
                .stream(constructorList.getAllSubterms())
                .map(this::readConstructor)
                .collect(Collectors.toList());

        return new Signature(constructors);
    }

    protected Constructor readConstructor(IStrategoTerm term) {
        if (term instanceof IStrategoAppl) {
            IStrategoAppl appl = (IStrategoAppl) term;

            if ("OpDecl".equals(appl.getConstructor().getName())) {
                String name = ((IStrategoString) appl.getSubterm(0)).stringValue();

                if (appl.getSubterm(1) instanceof IStrategoAppl) {
                    if ("ConstType".equals(((IStrategoAppl) appl.getSubterm(1)).getConstructor().getName())) {
                        Sort result = readSort(appl.getSubterm(1));

                        return new Constructor(name, Collections.emptyList(), result);
                    } else {
                        IStrategoTerm functionTerm = term.getSubterm(1);
                        IStrategoTerm argumentsTerm = functionTerm.getSubterm(0);
                        IStrategoTerm resultTerm = functionTerm.getSubterm(1);
                        List<Sort> arguments = readSortList(argumentsTerm);
                        Sort result = readSort(resultTerm);

                        return new Constructor(name, arguments, result);
                    }
                }
            }
        }

        throw new IllegalArgumentException("Could not read term as constructor: " + term);
    }

    protected List<Sort> readSortList(IStrategoTerm term) {
        if (term instanceof IStrategoList) {
            return Arrays
                    .stream(term.getAllSubterms())
                    .map(this::readSort)
                    .collect(Collectors.toList());
        }

        throw new IllegalArgumentException("Could not read term as list of sorts: " + term);
    }

    protected Sort readSort(IStrategoTerm term) {
        if (term instanceof IStrategoAppl) {
            IStrategoAppl appl = (IStrategoAppl) term;

            if ("ConstType".equals(appl.getConstructor().getName())) {
                String sortName = ((IStrategoString) term.getSubterm(0).getSubterm(0)).stringValue();

                return new Sort(sortName);
            }
        }

        throw new IllegalArgumentException("Could not read term as sort: " + term);
    }
}
