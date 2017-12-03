package org.metaborg.spg.sentence.antlr.grammar;

public class RangeConc implements Ranges {
    private final Ranges first;
    private final Ranges second;

    public RangeConc(Ranges first, Ranges second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int size() {
        return first.size() + second.size();
    }

    @Override
    public char get(int index) {
        int firstSize = first.size();

        if (index < firstSize) {
            return first.get(index);
        } else {
            return second.get(index - firstSize);
        }
    }
}
