package org.metaborg.spg.sentence.sdf3;

import java.util.Collection;
import java.util.stream.Stream;

public class Grammar {
    private final Collection<Module> modules;

    public Grammar(Collection<Module> modules) {
        this.modules = modules;
    }

    public Iterable<Module> getModules() {
        return modules;
    }

    public Stream<Production> getProductions() {
        return modules.stream().flatMap(Module::getProductions);
    }
}
