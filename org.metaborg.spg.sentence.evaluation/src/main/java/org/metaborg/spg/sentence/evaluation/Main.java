package org.metaborg.spg.sentence.evaluation;

import java.util.Arrays;
import java.util.Collection;

public class Main {
    public static void main(String[] args) {
        Collection<Subject> subjects = Arrays.asList(
                new Subject()
        );

        for (Subject subject : subjects) {
            evaluateSubject(subject);
        }
    }

    public static void evaluateSubject(Subject subject) {
        // TODO: One warm-up round, then measure:

            // TODO: terms until ambiguity
            // TODO: time until ambiguity
            // TODO: size of text that is ambiguous

            // TODO: steps until shrunken
            // TODO: time until shrunken
            // TODO: size of text that is ambiguous shrunken
    }
}
