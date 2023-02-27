package org.metaborg.spg.sentence.signature;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.spg.sentence.shared.utils.StreamUtils;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class Signature {
    private static final Logger logger = LoggerFactory.getLogger(Signature.class);

    private final Collection<Operation> operations;
    private final MultiSetMap.Immutable<String, Constructor> constructorCache;
    private final Map<Sort, Set<Sort>> injectionCache;

    public Signature(Collection<Operation> operations) {
        this.operations = operations;
        final MultiSetMap.Transient<String, Constructor> constructorCache = MultiSetMap.Transient.of();
        getConstructors().forEach(constructor -> constructorCache.put(constructor.getName(), constructor));
        this.constructorCache = constructorCache.freeze();
        this.injectionCache = getSorts().collect(Collectors.toMap(Function.identity(), this::injectionsTransitive));
    }

    public Collection<Operation> getOperations() {
        return operations;
    }

    public Stream<Sort> getSorts() {
        return operations.stream().flatMap(o -> o.getSorts().stream());
    }

    public Sort getSort(IStrategoTerm haystack, IStrategoTerm needle) {
        if (haystack instanceof IStrategoAppl) {
            IStrategoAppl appl = (IStrategoAppl) haystack;

            if ("Some".equals(appl.getConstructor().getName())) {
                return getSort(appl.getSubterm(0), needle);
            }
        }

        Sort sort = getConstructor(haystack).getResult();
        Optional<Sort> sortOpt = getSort(haystack, needle, sort);

        if (!sortOpt.isPresent()) {
            throw new IllegalArgumentException("Cannot get sort for needle " + needle + " in haystack " + haystack);
        }

        return sortOpt.get();
    }

    private Optional<Sort> getSort(IStrategoTerm haystack, IStrategoTerm needle, Sort sort) {
        if (haystack == needle) {
            return of(sort);
        }

        Iterable<Sort> arguments = getArguments(haystack, sort);
        Iterable<IStrategoTerm> terms = Arrays.asList(haystack.getAllSubterms());

        for (Tuple2<Sort, IStrategoTerm> sortTerm : Iterables2.zip(arguments, terms, Tuple2::of)) {
            Optional<Sort> sortOptional = getSort(sortTerm._2(), needle, sortTerm._1());

            if (sortOptional.isPresent()) {
                return sortOptional;
            }
        }

        return empty();
    }

    private Iterable<Sort> getArguments(IStrategoTerm term, Sort sort) {
        if (term instanceof IStrategoAppl) {
            IStrategoAppl appl = (IStrategoAppl) term;

            if ("Some".equals(appl.getConstructor().getName())) {
                return Collections.singleton(sort.getParameter());
            } else if ("None".equals(appl.getConstructor().getName())) {
                return Collections.emptySet();
            } else {
                return getConstructor(term).getArguments();
            }
        } else if (term instanceof IStrategoList) {
            IStrategoList list = (IStrategoList) term;

            if (list.size() > 0) {
                return Collections.nCopies(list.size(), sort.getParameter());
            } else {
                return Collections.emptySet();
            }
        } else if (term instanceof IStrategoString) {
            return Collections.emptySet();
        }

        throw new IllegalArgumentException("Unable to get arguments for term: " + term);
    }

    private Set<Constructor> getConstructors() {
        return operations.stream().filter(o -> o instanceof Constructor).map(o -> (Constructor) o)
            .collect(Collectors.toSet());
    }

    private Collection<Constructor> getConstructors(String name) {
        return constructorCache.get(name).toCollection();
    }

    private Constructor getConstructor(IStrategoTerm term) {
        if (term instanceof IStrategoAppl) {
            IStrategoAppl appl = (IStrategoAppl) term;

            // TODO: Cache constructors by name *and* arity so this becomes O(1)? In that case, getSymbol also becomes easier?
            for (Constructor constructor : getConstructors(appl.getConstructor().getName())) {
                if (constructor.getArity() == appl.getConstructor().getArity()) {
                    return constructor;
                }
            }
        }

        throw new IllegalArgumentException("Constructor for term " + term + " not found.");
    }

    public Stream<Injection> getInjections() {
        return operations.stream().filter(o -> o instanceof Injection).map(o -> (Injection) o);
    }

    public Set<Sort> getInjections(Sort sort) {
        return injectionCache.get(sort);
    }

    protected Stream<Sort> injections(Sort sort) {
        return getInjections()
                .filter(injection -> sort.equals(injection.getResult()))
                .map(Injection::getArgument);
    }

    private Set<Sort> injectionsTransitive(Sort sort) {
        return injectionsTransitiveStream(sort).collect(Collectors.toSet());
    }

    private Stream<Sort> injectionsTransitiveStream(Sort sort) {
        logger.trace("Compute transitive injections for sort {}", sort);

        Stream<Sort> sorts =
            injections(sort).flatMap(this::injectionsTransitiveStream);

        return StreamUtils.cons(sort, sorts);
    }
}
