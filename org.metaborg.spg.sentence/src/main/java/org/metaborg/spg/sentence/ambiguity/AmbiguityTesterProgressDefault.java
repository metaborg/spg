package org.metaborg.spg.sentence.ambiguity;

public class AmbiguityTesterProgressDefault implements AmbiguityTesterProgress {
    @Override
    public void sentenceGenerated(String text) {
        System.out.println("=== Program ===");
        System.out.println(text);
    }

    @Override
    public void sentenceShrinked(String text) {
        System.out.println("=== Shrink ==");
        System.out.println(text);
    }
}
