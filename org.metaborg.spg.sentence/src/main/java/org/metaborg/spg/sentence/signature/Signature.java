package org.metaborg.spg.sentence.signature;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.util.*;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.metaborg.spg.sentence.shared.utils.FunctionalUtils.zip;
import static org.metaborg.spg.sentence.shared.utils.SetUtils.cons;

public class Signature {
    private final Iterable<Operation> operations;
    private final Map<Sort, Iterable<Sort>> injectionCache;

    public Signature(Iterable<Operation> operations) {
        this.operations = operations;
        this.injectionCache = Maps.toMap(getSorts(), this::injectionsTransitive);
    }

    public Iterable<Operation> getOperations() {
        return operations;
    }

    public Iterable<Operation> getOperations(Sort sort) {
        return null;
    }

    public Set<Sort> getSorts() {
        return FluentIterable
                .from(operations)
                .transformAndConcat(Operation::getSorts)
                .toSet();
    }

    public Optional<Sort> getSort(IStrategoTerm haystack, IStrategoTerm needle, Sort sort) {
        if (haystack == needle) {
            return of(sort);
        }

        Constructor constructor = getConstructor(haystack, sort);
        List<IStrategoTerm> terms = Arrays.asList(haystack.getAllSubterms());
        List<Sort> arguments = constructor.getArguments();
        Iterable<Pair<Sort, IStrategoTerm>> sortTerms = zip(arguments, terms);

        for (Pair<Sort, IStrategoTerm> sortTerm : sortTerms) {
            Optional<Sort> sortOptional = getSort(sortTerm.getRight(), needle, sortTerm.getLeft());

            if (sortOptional.isPresent()) {
                return sortOptional;
            }
        }

        return empty();
    }

    private Set<Constructor> getConstructorsTransitive(Sort sort) {
        return FluentIterable
                .from(getInjections(sort))
                .transformAndConcat(this::getConstructors)
                .toSet();
    }

    private Iterable<Constructor> getConstructors(Sort sort) {
        return FluentIterable
                .from(operations)
                .filter(Constructor.class)
                .filter(constructor -> sort.equals(constructor.getResult()));
    }

    private Constructor getConstructor(IStrategoTerm term, Sort sort) {
        for (Constructor constructor : getConstructorsTransitive(sort)) {
            if (constructor.getArity() == term.getSubtermCount()) {
                return constructor;
            }
        }

        throw new IllegalArgumentException("Constructor for term " + term + " as sort " + sort + " not found.");
    }

    public Iterable<Injection> getInjections() {
        return Iterables.filter(operations, Injection.class);
    }

    public Iterable<Sort> getInjections(Sort sort) {
        return injectionCache.get(sort);
    }

    private Set<Sort> injections(Sort sort) {
        return FluentIterable
                .from(getInjections())
                .filter(injection -> sort.equals(injection.getResult()))
                .transform(Injection::getArgument)
                .toSet();
    }

    private Set<Sort> injectionsTransitive(Sort sort) {
        Set<Sort> sorts = FluentIterable
                .from(injections(sort))
                .transformAndConcat(this::injectionsTransitive)
                .toSet();

        return cons(sort, sorts);
    }
}
