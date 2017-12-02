package org.metaborg.spg.sentence.shared.utils;

import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Set;

public class SetUtils {
    public static <T> Set<T> cons(T x, Set<T> xs) {
        return Sets.union(Collections.singleton(x), xs);
    }
}
