package org.metaborg.spg.sentence.sdf.eclipse.job;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.ambiguity.TesterConfig;

public interface JobFactory {
    AmbiguityJob createAmbiguityJob(TesterConfig config, IProject project, ILanguageImpl language);
}
