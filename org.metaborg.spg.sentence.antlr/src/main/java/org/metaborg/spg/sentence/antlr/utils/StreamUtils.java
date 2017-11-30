package org.metaborg.spg.sentence.antlr.utils;

import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

public class StreamUtils {
    public static <T> Stream<T> cons(T head, Stream<? extends T> tail) {
        return concat(of(head), tail);
    }

    public static <T> Stream<T> snoc(Stream<? extends T> init, T last) {
        return concat(init, of(last));
    }
}
