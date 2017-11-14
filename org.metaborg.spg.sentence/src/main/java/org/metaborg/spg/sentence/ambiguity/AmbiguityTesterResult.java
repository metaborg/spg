package org.metaborg.spg.sentence.ambiguity;

public class AmbiguityTesterResult {
    private final int terms;
    private final long duration;
    private final String ambiguousText;

    public AmbiguityTesterResult(int terms, long duration, String ambiguousText) {
        this.terms = terms;
        this.duration = duration;
        this.ambiguousText = ambiguousText;
    }

    public AmbiguityTesterResult(int terms, long duration) {
        this.terms = terms;
        this.duration = duration;
        this.ambiguousText = null;
    }

    public int getTerms() {
        return terms;
    }

    public long getDuration() {
        return duration;
    }

    public String getAmbiguousText() {
        return ambiguousText;
    }

    public boolean foundAmbiguity() {
        return ambiguousText != null;
    }
}
