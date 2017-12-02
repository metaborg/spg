package org.metaborg.spg.sentence.shared.utils;

import com.google.common.collect.AbstractIterator;
import org.apache.commons.lang3.tuple.Pair;
import org.metaborg.spg.sentence.shared.functional.CheckedConsumer;
import org.metaborg.spg.sentence.shared.functional.CheckedFunction;
import org.metaborg.spg.sentence.shared.functional.CheckedPredicate;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class FunctionalUtils {
    public static <L, R> Iterable<Pair<L, R>> zip(Iterable<L> iterable1, Iterable<R> iterable2) {
        return () -> {
            final Iterator<L> iterator1 = iterable1.iterator();
            final Iterator<R> iterator2 = iterable2.iterator();

            return new AbstractIterator<Pair<L, R>>() {
                @Override
                protected Pair<L, R> computeNext() {
                    if (iterator1.hasNext() && iterator2.hasNext()) {
                        return Pair.of(iterator1.next(), iterator2.next());
                    } else {
                        return endOfData();
                    }
                }
            };
        };
    }

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

    public static <T, R, E extends Exception> com.google.common.base.Function<T, R> uncheck2(CheckedFunction<T, R, E> function) {
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
