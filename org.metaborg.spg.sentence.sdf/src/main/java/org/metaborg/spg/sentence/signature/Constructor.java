package org.metaborg.spg.sentence.signature;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Constructor extends Operation {
    private final String name;
    private final List<Sort> arguments;
    private final Sort result;

    public Constructor(String name, List<Sort> arguments, Sort result) {
        this.name = name;
        this.arguments = arguments;
        this.result = result;
    }

    public String getName() {
        return name;
    }

    public List<Sort> getArguments() {
        return arguments;
    }

    public Sort getArgument(int index) {
        return arguments.get(index);
    }

    public int getArity() {
        return arguments.size();
    }

    public Sort getResult() {
        return result;
    }

    public boolean equals(IStrategoConstructor constructor) {
        return constructor.getName().equals(name);
    }

    public boolean matches(IStrategoAppl appl) {
        return appl.getSubtermCount() == getArity() && equals(appl.getConstructor());
    }

    @Override
    public Set<Sort> getSorts() {
        Set<Sort> sorts = new HashSet<>(arguments);
        sorts.add(result);

        return sorts;
    }

    @Override
    public String toString() {
        return name + " : " + arguments.stream().map(Sort::toString).collect(Collectors.joining(" * ")) + " -> " + result;
    }
}
