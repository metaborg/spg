package org.metaborg.spg.sentence.signature;

import com.google.common.collect.Sets;

import java.util.Set;

public class Injection extends Operation {
    private final Sort argument;
    private final Sort result;

    public Injection(Sort argument, Sort result) {
        this.argument = argument;
        this.result = result;
    }

    public Sort getArgument() {
        return argument;
    }

    public Sort getResult() {
        return result;
    }

    @Override
    public Set<Sort> getSorts() {
        return Sets.newHashSet(argument, result);
    }

    @Override
    public String toString() {
        return " : " + argument + " -> " + result;
    }
}
