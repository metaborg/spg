package org.metaborg.spg.sentence.utils;

import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;

public class StreamUtils {
    public static <T> Stream<T> cons(T head, Stream<? extends T> tail) {
        return concat(of(head), tail);
    }

    public static <T> Stream<T> o2s(Optional<T> optional) {
        if (optional.isPresent()) {
            return of(optional.get());
        } else {
            return empty();
        }
    }
}
