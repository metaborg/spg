package org.metaborg.spg.sentence.signature;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.spg.sentence.sdf3.Grammar;
import org.metaborg.spg.sentence.sdf3.Production;
import org.metaborg.spg.sentence.sdf3.symbol.Iter;
import org.metaborg.spg.sentence.sdf3.symbol.IterSep;
import org.metaborg.spg.sentence.sdf3.symbol.IterStar;
import org.metaborg.spg.sentence.sdf3.symbol.IterStarSep;
import org.metaborg.spg.sentence.sdf3.symbol.Nonterminal;
import org.metaborg.spg.sentence.sdf3.symbol.Opt;
import org.metaborg.spg.sentence.sdf3.symbol.Symbol;

public class SignatureFactory {
    public Signature create(Grammar grammar) {
        Collection<Operation> operations = grammar.getProductions()
                .filter(Production::isNotBracket)
                .filter(Production::isNotReject)
                .map(this::createOperation)
                .collect(Collectors.toList());

        return new Signature(operations);
    }

    private Operation createOperation(Production production) {
        Optional<String> constructorOpt = production.getConstructor();

        if (constructorOpt.isPresent()) {
            String name = constructorOpt.get();
            Sort sort = createSort(production.getLhs());
            List<Sort> arguments = createSorts(production.getRhs());

            return new Constructor(name, arguments, sort);
        } else {
            Sort argument = createSort(production.getRhs().iterator().next());
            Sort result = createSort(production.getLhs());

            return new Injection(argument, result);
        }
    }

    private List<Sort> createSorts(Stream<Symbol> symbols) {
        return symbols.map(this::createSort)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
