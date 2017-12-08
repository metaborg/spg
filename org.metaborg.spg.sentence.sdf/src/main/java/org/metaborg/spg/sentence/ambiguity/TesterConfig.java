package org.metaborg.spg.sentence.ambiguity;

public class TesterConfig {
    private final int maxNumberOfTerms;
    private final int maxTermSize;

    public TesterConfig(int maxNumberOfTerms, int maxTermSize) {
        this.maxNumberOfTerms = maxNumberOfTerms;
        this.maxTermSize = maxTermSize;
    }

    public int getMaxNumberOfTerms() {
        return maxNumberOfTerms;
    }

    public int getMaxTermSize() {
        return maxTermSize;
    }
}
