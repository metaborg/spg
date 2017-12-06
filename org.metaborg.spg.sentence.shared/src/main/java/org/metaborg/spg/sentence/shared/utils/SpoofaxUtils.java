package org.metaborg.spg.sentence.shared.utils;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.spoofax.core.Spoofax;

import java.io.File;

public class SpoofaxUtils {
    public static ILanguageImpl loadLanguage(Spoofax spoofax, String file) throws MetaborgException {
        return loadLanguage(spoofax, new File(file));
    }

    public static ILanguageImpl loadLanguage(Spoofax spoofax, File file) throws MetaborgException {
        FileObject languageLocation = spoofax.resourceService.resolve(file);

        return spoofax.languageDiscoveryService.languageFromArchive(languageLocation);
    }

    public static IProject getOrCreateProject(Spoofax spoofax, String file) throws MetaborgException {
        return getOrCreateProject(spoofax, new File(file));
    }

    public static IProject getOrCreateProject(Spoofax spoofax, File file) throws MetaborgException {
        SimpleProjectService simpleProjectService = spoofax.injector.getInstance(SimpleProjectService.class);
        FileObject resource = spoofax.resourceService.resolve(file);
        IProject project = simpleProjectService.get(resource);

        if (project == null) {
            return simpleProjectService.create(resource);
        } else {
            return project;
        }
    }
}
