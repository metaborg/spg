package org.metaborg.spg.sentence.ambiguity.result;

public class TestResult {
    private final FindResult findResult;
    private final ShrinkResult shrinkResult;

    public TestResult(FindResult findResult) {
        this(findResult, null);
    }

    public TestResult(FindResult findResult, ShrinkResult shrinkResult) {
        this.findResult = findResult;
        this.shrinkResult = shrinkResult;
    }

    public FindResult getFindResult() {
        return findResult;
    }

    public ShrinkResult getShrinkResult() {
        return shrinkResult;
    }

    public boolean shrunk() {
        return shrinkResult != null;
    }
}
