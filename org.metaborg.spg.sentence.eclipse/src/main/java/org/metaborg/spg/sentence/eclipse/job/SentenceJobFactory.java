package org.metaborg.spg.sentence.eclipse.job;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.eclipse.config.SentenceHandlerConfig;
import org.metaborg.spg.sentence.eclipse.job.SentenceJob;

import com.google.inject.assistedinject.Assisted;

public interface SentenceJobFactory {
    SentenceJob createSentenceJob(
    		IProject project,
    		@Assisted("language") ILanguageImpl language,
    		@Assisted("strategoLanguage") ILanguageImpl strategoLanguage,
    		SentenceHandlerConfig config);
}
