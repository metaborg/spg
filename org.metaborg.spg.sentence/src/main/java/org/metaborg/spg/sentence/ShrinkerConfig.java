package org.metaborg.spg.sentence;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spg.sentence.signature.Signature;

public class ShrinkerConfig {
    private final ILanguageImpl language;
    private final Signature signature;
    private final String rootSort;
    private final PrettyPrinter prettyPrinter;

    public ShrinkerConfig(ILanguageImpl language, Signature signature, String rootSort, PrettyPrinter prettyPrinter) {
        this.language = language;
        this.signature = signature;
        this.rootSort = rootSort;
        this.prettyPrinter = prettyPrinter;
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

    public PrettyPrinter getPrettyPrinter() {
        return prettyPrinter;
    }
}
