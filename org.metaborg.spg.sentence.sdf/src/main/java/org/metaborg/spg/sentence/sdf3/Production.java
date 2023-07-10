package org.metaborg.spg.sentence.sdf3;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.spg.sentence.sdf3.attribute.Attribute;
import org.metaborg.spg.sentence.sdf3.attribute.Bracket;
import org.metaborg.spg.sentence.sdf3.attribute.Reject;
import org.metaborg.spg.sentence.sdf3.symbol.Nonterminal;
import org.metaborg.spg.sentence.sdf3.symbol.Symbol;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public class Production {
    private final Nonterminal lhs;
    private final Stream<Symbol> rhs;
    private final Stream<Attribute> attributes;
    private final String constructor;

    public Production(Nonterminal lhs, Stream<Symbol> rhs, Stream<Attribute> attributes) {
        this(lhs, rhs, attributes, null);
    }

    public Production(Nonterminal lhs, Stream<Symbol> rhs, Stream<Attribute> attributes, String constructor) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.attributes = attributes;
        this.constructor = constructor;
    }

    public Nonterminal getLhs() {
        return lhs;
    }

    public Stream<Symbol> getRhs() {
        return rhs;
    }

    public boolean isReject() {
        return hasAttribute(Reject.class);
    }

    public boolean isNotReject() {
        return !isReject();
    }

    public boolean isBracket() {
        return hasAttribute(Bracket.class);
    }

    public boolean isNotBracket() {
        return !isBracket();
    }

    public Optional<String> getConstructor() {
        return ofNullable(constructor);
    }

    private boolean hasAttribute(Class<?> clazz) {
        Optional<Attribute> attributeOptional = getAttribute(clazz::isInstance);

        return attributeOptional.isPresent();
    }

    private Optional<Attribute> getAttribute(Predicate<Attribute> predicate) {
        for (Attribute attribute : (Iterable<Attribute>) attributes::iterator) {
            if (predicate.test(attribute)) {
                return of(attribute);
            }
        }

        return empty();
    }

    @Override
    public String toString() {
        final String rhsString = rhs.map(Symbol::toString).collect(Collectors.joining(" "));
        if (constructor != null) {
            return lhs + "." + constructor + " = " + rhsString;
        } else {
            return lhs + " = " + rhsString;
        }
    }
}
