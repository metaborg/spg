package org.metaborg.spg.sentence.signature;

import com.google.common.collect.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.util.*;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.metaborg.spg.sentence.shared.utils.FunctionalUtils.zip;
import static org.metaborg.spg.sentence.shared.utils.SetUtils.cons;

public class Signature {
    private static final Logger logger = LoggerFactory.getLogger(Signature.class);

    private final Iterable<Operation> operations;
    private final Multimap<String, Constructor> constructorCache;
    private final Map<Sort, Set<Sort>> injectionCache;

    public Signature(Iterable<Operation> operations) {
        this.operations = operations;
        this.constructorCache = Multimaps.index(getConstructors(), Constructor::getName);
        this.injectionCache = Maps.toMap(getSorts(), this::injectionsTransitive);
    }

    public Iterable<Operation> getOperations() {
        return operations;
    }

    public Set<Sort> getSorts() {
        return FluentIterable
                .from(operations)
                .transformAndConcat(Operation::getSorts)
                .toSet();
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

        for (Pair<Sort, IStrategoTerm> sortTerm : zip(arguments, terms)) {
            Optional<Sort> sortOptional = getSort(sortTerm.getRight(), needle, sortTerm.getLeft());

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
        return FluentIterable
                .from(operations)
                .filter(Constructor.class)
                .toSet();
    }

    private Collection<Constructor> getConstructors(String name) {
        return constructorCache.get(name);
    }

    private Constructor getConstructor(IStrategoTerm term) {
        if (term instanceof IStrategoAppl) {
            IStrategoAppl appl = (IStrategoAppl) term;

            // TODO: Cache constructors by name *and* arity so this becomes O(1)?
              // TODO: In that case, getSort also becomes easier?
            for (Constructor constructor : getConstructors(appl.getConstructor().getName())) {
                if (constructor.getArity() == appl.getConstructor().getArity()) {
                    return constructor;
                }
            }
        }

        throw new IllegalArgumentException("Constructor for term " + term + " not found.");
    }

    public Iterable<Injection> getInjections() {
        return Iterables.filter(operations, Injection.class);
    }

    public Set<Sort> getInjections(Sort sort) {
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
        logger.trace("Compute transitive injections for sort {}", sort);

        Set<Sort> sorts = FluentIterable
                .from(injections(sort))
                .transformAndConcat(this::injectionsTransitive)
                .toSet();

        return cons(sort, sorts);
    }
}
