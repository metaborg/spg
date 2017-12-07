package org.metaborg.spg.sentence.ambiguity;

import java.util.ArrayList;
import java.util.List;

public abstract class StatisticsTesterProgress implements TesterProgress {
    private ArrayList<Integer> lengths;

    public StatisticsTesterProgress() {
        this.lengths = new ArrayList<>();
    }

    @Override
    public void sentenceGenerated(String text) {
        lengths.add(text.length());
    }

    public List<Integer> getLengths() {
        return lengths;
    }
}
