package org.metaborg.spg.sentence.evaluation;

import java.io.File;

public class Subject {
    private final String name;
    private final String languagePath;
    private final String projectPath;

    public Subject(String name, String languagePath, String projectPath) {
        this.name = name;
        this.languagePath = languagePath;
        this.projectPath = projectPath;
    }

    @Override
    public String toString() {
        return name;
    }

    public File getLanguageFile() {
        return new File(languagePath);
    }

    public File getProjectFile() {
        return new File(projectPath);
    }
}
