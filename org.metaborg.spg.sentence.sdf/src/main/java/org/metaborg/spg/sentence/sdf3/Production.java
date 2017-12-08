package org.metaborg.spg.sentence.sdf3;

import com.google.common.base.Joiner;
import org.metaborg.spg.sentence.sdf3.attribute.Attribute;
import org.metaborg.spg.sentence.sdf3.attribute.Bracket;
import org.metaborg.spg.sentence.sdf3.attribute.Reject;
import org.metaborg.spg.sentence.sdf3.symbol.Nonterminal;
import org.metaborg.spg.sentence.sdf3.symbol.Symbol;

import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public class Production {
    private final Nonterminal lhs;
    private final Iterable<Symbol> rhs;
    private final Iterable<Attribute> attributes;
    private final String constructor;

    public Production(Nonterminal lhs, Iterable<Symbol> rhs, Iterable<Attribute> attributes) {
        this(lhs, rhs, attributes, null);
    }

    public Production(Nonterminal lhs, Iterable<Symbol> rhs, Iterable<Attribute> attributes, String constructor) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.attributes = attributes;
        this.constructor = constructor;
    }

    public Nonterminal getLhs() {
        return lhs;
    }

    public Iterable<Symbol> getRhs() {
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
        for (Attribute attribute : attributes) {
            if (predicate.test(attribute)) {
                return of(attribute);
            }
        }

        return empty();
    }

    @Override
    public String toString() {
        if (constructor != null) {
            return lhs + "." + constructor + " = " + Joiner.on(" ").join(rhs);
        } else {
            return lhs + " = " + Joiner.on(" ").join(rhs);
        }
    }
}
