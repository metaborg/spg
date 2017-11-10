package org.metaborg.spg.sentence.eclipse.config;

public class SentenceHandlerConfig {
    private int limit;
    private int maxSize;

    public SentenceHandlerConfig(int limit, int maxSize) {
        this.limit = limit;
        this.maxSize = maxSize;
    }

    public int getLimit() {
        return limit;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
