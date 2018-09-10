package org.metaborg.spg.sentence.shared.utils;

import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.core.Spoofax;

public class SpoofaxUtils {

    public static ILanguageImpl getLanguage(Spoofax spoofax, String name) {
        ILanguage language = spoofax.languageService.getLanguage(name);
        if(language == null) {
            throw new MetaborgRuntimeException("Language " + name + " not found.");
        }
        ILanguageImpl impl = language.activeImpl();
        if(impl == null) {
            throw new MetaborgRuntimeException("Language " + name + " has no active implementation.");
        }
        return impl;
    }

}