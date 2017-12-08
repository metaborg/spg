package org.metaborg.spg.sentence.antlr.eclipse.config;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;

public class DifferenceJobConfig {
    private final ILanguageImpl language;
    private final IProject project;
    private final int maxNumberOfTerms;
    private final int maxTermSize;
    private final String antlrGrammar;
    private final String antlrStartSymbol;

    public DifferenceJobConfig(
            ILanguageImpl language,
            IProject project,
            int maxNumberOfTerms,
            int maxTermSize,
            String antlrGrammar,
            String antlrStartSymbol) {
        this.language = language;
        this.project = project;
        this.maxNumberOfTerms = maxNumberOfTerms;
        this.maxTermSize = maxTermSize;
        this.antlrGrammar = antlrGrammar;
        this.antlrStartSymbol = antlrStartSymbol;
    }

    public ILanguageImpl getLanguage() {
        return language;
    }

    public IProject getProject() {
        return project;
    }

    public int getMaxNumberOfTerms() {
        return maxNumberOfTerms;
    }

    public int getMaxTermSize() {
        return maxTermSize;
    }

    public String getAntlrGrammar() {
        return antlrGrammar;
    }

    public String getAntlrStartSymbol() {
        return antlrStartSymbol;
    }
}
