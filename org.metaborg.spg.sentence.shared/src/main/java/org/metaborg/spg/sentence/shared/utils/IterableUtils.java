package org.metaborg.spg.sentence.shared.utils;

import com.google.common.collect.Iterables;

import java.util.Collections;

public class IterableUtils {
    public static <T> Iterable<T> cons(T head, Iterable<T> tail) {
        return Iterables.concat(Collections.singletonList(head), tail);
    }

    public static <T> Iterable<T> snoc(Iterable<T> init, T last) {
        return Iterables.concat(init, Collections.singleton(last));
    }

    public static <T> T getFirst(Iterable<T> iterable) {
        return Iterables.getFirst(iterable, null);
    }
}
