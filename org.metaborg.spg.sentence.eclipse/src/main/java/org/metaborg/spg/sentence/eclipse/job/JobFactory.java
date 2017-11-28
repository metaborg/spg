package org.metaborg.spg.sentence.eclipse.job;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.ambiguity.AmbiguityTesterConfig;

public interface JobFactory {
    AmbiguityJob createAmbiguityJob(AmbiguityTesterConfig config, IProject project, ILanguageImpl language);

    LiberalDifferenceJob createLiberalDifferenceJob(IProject project, ILanguageImpl languageImpl);

    RestrictiveDifferenceJob createRestrictiveDifferenceJob(IProject project, ILanguageImpl languageImpl);
}
