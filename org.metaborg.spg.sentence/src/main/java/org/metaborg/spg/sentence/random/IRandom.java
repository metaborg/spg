package org.metaborg.spg.sentence.random;

import java.util.List;

public interface IRandom {
    <T> T fromList(List<T> list);

    int fromRange(int bound);

    boolean flip();

    <T> List<T> shuffle(List<T> list);
}
