package org.metaborg.spg.sentence.eclipse.job;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.ambiguity.AmbiguityTesterConfig;

public interface SentenceJobFactory {
    SentenceJob createSentenceJob(IProject project, ILanguageImpl language, AmbiguityTesterConfig config);
}
