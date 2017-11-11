package org.metaborg.spg.sentence.functional;

@FunctionalInterface
public interface CheckedConsumer<T, E extends Exception> {
    void accept(T element) throws E;
}
