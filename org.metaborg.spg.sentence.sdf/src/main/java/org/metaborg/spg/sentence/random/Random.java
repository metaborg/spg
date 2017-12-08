package org.metaborg.spg.sentence.random;

import java.util.Collections;
import java.util.List;

public class Random implements IRandom {
    private final java.util.Random random;

    public Random() {
        random = new java.util.Random();
    }

    public Random(long seed) {
        random = new java.util.Random(seed);
    }

    @Override
    public <T> T fromList(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    @Override
    public int fromRange(int bound) {
        return random.nextInt(bound);
    }

    @Override
    public boolean flip() {
        return random.nextInt(2) == 0;
    }

    @Override
    public <T> List<T> shuffle(List<T> list) {
        Collections.shuffle(list, random);

        return list;
    }
}
