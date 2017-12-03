package org.metaborg.spg.sentence.sdf3;

import com.google.common.collect.FluentIterable;

public class Module {
    private final String name;
    private final Iterable<String> imports;
    private final Iterable<Section> sections;

    public Module(String name, Iterable<String> imports, Iterable<Section> sections) {
        this.name = name;
        this.imports = imports;
        this.sections = sections;
    }

    public String getName() {
        return name;
    }

    public Iterable<String> getImports() {
        return imports;
    }

    public Iterable<Section> getSections() {
        return sections;
    }

    public Iterable<Production> getProductions() {
        return FluentIterable
                .from(sections)
                .transformAndConcat(Section::getProductions);
    }
}
