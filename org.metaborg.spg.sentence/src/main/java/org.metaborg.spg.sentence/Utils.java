package org.metaborg.spg.sentence;

import java.util.function.Predicate;

public class Utils {
    public static <T> Predicate<T> uncheckException(Shrinker.CheckedPredicate<T, Exception> function) {
        return element -> {
            try {
                return function.test(element);
            } catch (Exception ex) {
                // thanks to Christian Schneider for pointing out
                // that unchecked exceptions need not be wrapped again
                if (ex instanceof RuntimeException)
                    throw (RuntimeException) ex;
                else
                    throw new RuntimeException(ex);
            }
        };
    }
}
