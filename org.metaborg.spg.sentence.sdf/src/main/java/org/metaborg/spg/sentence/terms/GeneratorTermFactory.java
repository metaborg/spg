package org.metaborg.spg.sentence.terms;

import java.util.List;

import org.metaborg.sdf2table.grammar.ISymbol;
import org.metaborg.sdf2table.grammar.Symbol;
import org.metaborg.spg.sentence.generator.GeneratorAttachment;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.StrategoConstructor;
import org.spoofax.terms.attachments.AbstractWrappedTermFactory;

public class GeneratorTermFactory extends AbstractWrappedTermFactory {
    private final ITermFactory baseFactory;

    public GeneratorTermFactory(ITermFactory baseFactory) {
        super(baseFactory);

        this.baseFactory = baseFactory;
    }

    public IStrategoString makeString(ISymbol symbol, String text) {
        IStrategoString string = baseFactory.makeString(text);
        string.putAttachment(new GeneratorAttachment(symbol));

        return string;
    }

    public IStrategoTerm makeSome(Symbol symbol, IStrategoTerm term) {
        IStrategoAppl appl = makeAppl(makeConstructor("Some", 1), term);
        appl.putAttachment(new GeneratorAttachment(symbol));

        return appl;
    }

    public IStrategoTerm makeNone(Symbol symbol) {
        IStrategoAppl appl = makeAppl(makeConstructor("None", 0));
        appl.putAttachment(new GeneratorAttachment(symbol));

        return appl;
    }

    public IStrategoAppl makeAppl(ISymbol symbol, String constructorName, List<IStrategoTerm> children) {
        IStrategoTerm[] terms = new IStrategoTerm[children.size()];
        IStrategoAppl appl = makeAppl(constructorName, children.toArray(terms), symbol);
        appl.putAttachment(new GeneratorAttachment(symbol));

        return appl;
    }

    public IStrategoAppl makeAppl(String constructorName, IStrategoTerm[] children, ISymbol symbol) {
        IStrategoConstructor constructor = new StrategoConstructor(constructorName, children.length);
        IStrategoAppl appl = baseFactory.makeAppl(constructor, children, null);
        appl.putAttachment(new GeneratorAttachment(symbol));

        return appl;
    }

    public IStrategoList makeList(Symbol symbol, IStrategoTerm... terms) {
        IStrategoList list = baseFactory.makeList(terms);
        list.putAttachment(new GeneratorAttachment(symbol));

        return list;
    }

    public IStrategoList makeListCons(Symbol symbol, IStrategoTerm head, IStrategoList tail) {
        IStrategoList list = baseFactory.makeListCons(head, tail);
        list.putAttachment(new GeneratorAttachment(symbol));

        return list;
    }

    public IStrategoAppl replaceAppl(IStrategoTerm[] children, IStrategoAppl oldAppl) {
        return replaceAppl(oldAppl.getConstructor(), children, oldAppl);
    }

    @Override
    public IStrategoAppl replaceAppl(IStrategoConstructor constructor, IStrategoTerm[] children,
        IStrategoAppl oldAppl) {
        IStrategoAppl newAppl = baseFactory.replaceAppl(constructor, children, oldAppl);

        return (IStrategoAppl) baseFactory.copyAttachments(oldAppl, newAppl);
    }

    @Override
    public IStrategoList replaceList(IStrategoTerm[] children, IStrategoList oldList) {
        IStrategoList newList = baseFactory.replaceList(children, oldList);

        return (IStrategoList) baseFactory.copyAttachments(oldList, newList);
    }
}
