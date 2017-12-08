package org.metaborg.spg.sentence.signature;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import org.metaborg.spg.sentence.sdf3.Grammar;
import org.metaborg.spg.sentence.sdf3.Production;
import org.metaborg.spg.sentence.sdf3.symbol.*;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singleton;
import static org.metaborg.spg.sentence.shared.utils.IterableUtils.getFirst;

public class SignatureFactory {
    public Signature create(Grammar grammar) {
        Iterable<Operation> operations = FluentIterable
                .from(grammar.getProductions())
                .filter(Production::isNotBracket)
                .filter(Production::isNotReject)
                .transformAndConcat(this::createOperation);

        return new Signature(operations);
    }

    private Iterable<Operation> createOperation(Production production) {
        Optional<String> constructorOpt = production.getConstructor();

        if (constructorOpt.isPresent()) {
            String name = constructorOpt.get();
            Sort sort = createSort(production.getLhs());
            List<Sort> arguments = createSorts(production.getRhs());
            Constructor constructor = new Constructor(name, arguments, sort);

            return singleton(constructor);
        } else {
            Sort argument = createSort(getFirst(production.getRhs()));
            Sort result = createSort(production.getLhs());
            Injection injection = new Injection(argument, result);

            return singleton(injection);
        }
    }

    private List<Sort> createSorts(Iterable<Symbol> symbols) {
        return FluentIterable
                .from(symbols)
                .transform(this::createSort)
                .filter(Predicates.notNull())
                .toList();
    }

    private Sort createSort(Symbol symbol) {
        if (symbol instanceof Nonterminal) {
            return new Sort(((Nonterminal) symbol).getName());
        } else if (symbol instanceof Iter) {
            Sort parameter = createSort(((Iter) symbol).getSymbol());

            return new Sort("Iter", parameter);
        } else if (symbol instanceof IterSep) {
            Sort parameter = createSort(((IterSep) symbol).getSymbol());

            return new Sort("Iter", parameter);
        } else if (symbol instanceof IterStar) {
            Sort parameter = createSort(((IterStar) symbol).getSymbol());

            return new Sort("IterStar", parameter);
        } else if (symbol instanceof IterStarSep) {
            Sort parameter = createSort(((IterStarSep) symbol).getSymbol());

            return new Sort("IterStar", parameter);
        } else if (symbol instanceof Opt) {
            Sort parameter = createSort(((Opt) symbol).getSymbol());

            return new Sort("Option", parameter);
        }

        return null;
    }
}
