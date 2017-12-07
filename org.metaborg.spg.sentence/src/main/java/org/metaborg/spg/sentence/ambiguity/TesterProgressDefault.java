package org.metaborg.spg.sentence.ambiguity;

public class TesterProgressDefault extends StatisticsTesterProgress {
    @Override
    public void sentenceGenerated(String text) {
        super.sentenceGenerated(text);

        System.out.println("=== Program ===");
        System.out.println(text);
    }

    @Override
    public void sentenceShrinked(String text) {
        System.out.println("=== Shrink (" + text.length() + " chars) ===");
        System.out.println(text);
    }
}
