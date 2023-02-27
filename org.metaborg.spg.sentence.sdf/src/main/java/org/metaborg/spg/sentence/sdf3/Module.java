package org.metaborg.spg.sentence.sdf3;

import java.util.Collection;
import java.util.stream.Stream;

public class Module {
    private final String name;
    private final Collection<String> imports;
    private final Collection<Section> sections;

    public Module(String name, Collection<String> imports, Collection<Section> sections) {
        this.name = name;
        this.imports = imports;
        this.sections = sections;
    }

    public String getName() {
        return name;
    }

    public Collection<String> getImports() {
        return imports;
    }

    public Collection<Section> getSections() {
        return sections;
    }

    public Stream<Production> getProductions() {
        return sections.stream().flatMap(s -> s.getProductions().stream());
    }
}
