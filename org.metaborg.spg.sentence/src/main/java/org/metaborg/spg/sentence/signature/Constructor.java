package org.metaborg.spg.sentence.signature;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

import static org.metaborg.spg.sentence.shared.utils.IterableUtils.snoc;

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

    @Override
    public Set<Sort> getSorts() {
        Iterable<Sort> sorts = snoc(arguments, result);

        return Sets.newHashSet(sorts);
    }

    @Override
    public String toString() {
        return name + " : " + Joiner.on(" * ").join(arguments) + " -> " + result;
    }
}
