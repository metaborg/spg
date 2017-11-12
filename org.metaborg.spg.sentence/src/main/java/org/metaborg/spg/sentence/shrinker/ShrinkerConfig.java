package org.metaborg.spg.sentence.shrinker;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spg.sentence.printer.Printer;

public class ShrinkerConfig {
    private final ILanguageImpl language;
    private final Printer printer;

    public ShrinkerConfig(ILanguageImpl language, Printer printer) {
        this.language = language;
        this.printer = printer;
    }

    public ILanguageImpl getLanguage() {
        return language;
    }

    public Printer getPrinter() {
        return printer;
    }
}
