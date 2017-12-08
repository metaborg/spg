package org.metaborg.spg.sentence.sdf3;

import com.google.common.collect.FluentIterable;

import java.util.Collection;

public class Grammar {
    private final Collection<Module> modules;

    public Grammar(Collection<Module> modules) {
        this.modules = modules;
    }

    public Iterable<Module> getModules() {
        return modules;
    }

    public Iterable<Production> getProductions() {
        return FluentIterable
                .from(modules)
                .transformAndConcat(Module::getProductions);
    }
}
