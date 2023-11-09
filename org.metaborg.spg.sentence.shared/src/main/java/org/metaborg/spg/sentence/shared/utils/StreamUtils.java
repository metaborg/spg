package org.metaborg.spg.sentence.shared.utils;

import org.metaborg.spg.sentence.shared.functional.Pair;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class StreamUtils {
    public static <T> Stream<T> cons(T head, Stream<? extends T> tail) {
        return Stream.concat(Stream.of(head), tail);
    }

    public static <T> Stream<T> snoc(Stream<? extends T> init, T last) {
        return Stream.concat(init, Stream.of(last));
    }

    public static <T> Stream<T> o2s(Optional<T> optional) {
        return optional.map(Stream::of).orElseGet(Stream::empty);
    }

    public static <T, R> Stream<Pair<T, R>> zipWith(Stream<T> stream, Function<? super T, R> function) {
        return stream.map(e -> new Pair<>(e, function.apply(e)));
    }
}
