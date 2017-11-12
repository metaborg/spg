package org.metaborg.spg.sentence.shrinker;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.signature.Signature;

public class ShrinkerConfig {
    private final ILanguageImpl language;
    private final Signature signature;
    private final String rootSort;
    private final Printer printer;

    public ShrinkerConfig(ILanguageImpl language, Signature signature, String rootSort, Printer printer) {
        this.language = language;
        this.signature = signature;
        this.rootSort = rootSort;
        this.printer = printer;
    }

    public ILanguageImpl getLanguage() {
        return language;
    }

    public Signature getSignature() {
        return signature;
    }

    public String getRootSort() {
        return rootSort;
    }

    public Printer getPrinter() {
        return printer;
    }
}
