package org.metaborg.spg.sentence.signature;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SignatureTest {
    @Test
    public void testSorts() {
        Signature signature = new Signature(Arrays.asList(
                inj(sort("Foo"), sort("Bar")),
                inj(sort("Baz"), sort("Bar"))
        ));

        Set<Sort> sorts = signature.getSorts();

        assertEquals(new HashSet<>(Arrays.asList(sort("Foo"), sort("Bar"), sort("Baz"))), sorts);
    }

    @Test
    public void testInjection() {
        Signature signature = new Signature(Arrays.asList(
                inj(sort("Foo"), sort("Bar")),
                inj(sort("Baz"), sort("Bar"))
        ));

        Set<Sort> subsorts = signature.injections(sort("Bar"));

        assertEquals(new HashSet<>(Arrays.asList(sort("Foo"), sort("Baz"))), subsorts);
    }

    @Test
    public void testTransitiveInjection() {
        Signature signature = new Signature(Arrays.asList(
                inj(sort("Foo"), sort("Bar")),
                inj(sort("Baz"), sort("Bar"))
        ));

        Set<Sort> subsorts = signature.getInjections(sort("Bar"));

        assertEquals(new HashSet<>(Arrays.asList(sort("Foo"), sort("Bar"), sort("Baz"))), subsorts);
    }

    private Injection inj(Sort s1, Sort s2) {
        return new Injection(s1, s2);
    }

    private Sort sort(String name) {
        return new Sort(name);
    }
}
