package org.metaborg.spg.sentence.antlr.functional;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;

public class Utils {
    public static <T> Predicate<T> uncheck(CheckedPredicate<T, Exception> function) {
        return element -> {
            try {
                return function.test(element);
            } catch (Exception ex) {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    public static <U> Stream<U> lift(Optional<U> optional) {
        if (optional.isPresent()) {
            return of(optional.get());
        } else {
            return empty();
        }
    }
}
