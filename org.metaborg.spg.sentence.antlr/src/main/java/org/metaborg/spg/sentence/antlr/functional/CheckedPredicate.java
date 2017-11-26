package org.metaborg.spg.sentence.antlr.functional;

@FunctionalInterface
public interface CheckedPredicate<T, E extends Exception> {
    boolean test(T element) throws E;
}
