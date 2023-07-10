package org.metaborg.spg.sentence.signature;

import java.util.Arrays;
import java.util.HashSet;
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
        return new HashSet<>(Arrays.asList(argument, result));
    }

    @Override
    public String toString() {
        return " : " + argument + " -> " + result;
    }
}
