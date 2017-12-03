package org.metaborg.spg.sentence.eclipse.job;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.ambiguity.TesterConfig;
import org.metaborg.spg.sentence.eclipse.config.DifferenceJobConfig;

public interface JobFactory {
    AmbiguityJob createAmbiguityJob(TesterConfig config, IProject project, ILanguageImpl language);

    LiberalDifferenceJob createLiberalDifferenceJob(DifferenceJobConfig config);

    RestrictiveDifferenceJob createRestrictiveDifferenceJob(DifferenceJobConfig config);
}
