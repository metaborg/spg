package org.metaborg.spg.sentence.shared.utils;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.metaborg.spg.sentence.shared.functional.CheckedConsumer;
import org.metaborg.spg.sentence.shared.functional.CheckedFunction;
import org.metaborg.spg.sentence.shared.functional.CheckedPredicate;
import org.metaborg.util.tuple.Tuple2;

public class FunctionalUtils {

    public static <T> Predicate<T> uncheckPredicate(CheckedPredicate<T, Exception> function) {
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

    public static <T> Consumer<T> uncheckConsumer(CheckedConsumer<T, Exception> function) {
        return element -> {
            try {
                function.accept(element);
            } catch (Exception ex) {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    public static <T, R> Function<T, R> uncheck(CheckedFunction<T, R, Exception> function) {
        return element -> {
            try {
                return function.apply(element);
            } catch (Exception ex) {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    public static <T, R, E extends Exception> Function<T, R> uncheck2(CheckedFunction<T, R, E> function) {
        return element -> {
            try {
                return function.apply(element);
            } catch (Exception ex) {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
