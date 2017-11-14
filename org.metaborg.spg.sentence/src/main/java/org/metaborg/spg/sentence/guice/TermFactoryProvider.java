package org.metaborg.spg.sentence.guice;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.spoofax.interpreter.terms.ITermFactory;

public class TermFactoryProvider implements Provider<ITermFactory> {
    private ITermFactoryService termFactoryService;

    @Inject
    public TermFactoryProvider(ITermFactoryService termFactoryService) {
        this.termFactoryService = termFactoryService;
    }

    @Override
    public ITermFactory get() {
        return termFactoryService.getGeneric();
    }
}
