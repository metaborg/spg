package org.metaborg.spg.sentence.functional;

@FunctionalInterface
public interface CheckedFunction<T, R, E extends Exception> {
    R apply(T t) throws E;
}
