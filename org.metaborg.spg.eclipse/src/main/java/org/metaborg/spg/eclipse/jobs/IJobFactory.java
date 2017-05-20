package org.metaborg.spg.eclipse.jobs;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.core.Config;

public interface IJobFactory {
	/**
	 * Create a genearte job.
	 * 
	 * @param project
	 * @param language
	 * @param config
	 * @return
	 */
	public GenerateJob createGenerateJob(IProject project, ILanguageImpl language, Config config);
	
	/**
	 * Create an ambiguity job.
	 * 
	 * @param project
	 * @param language
	 * @param config
	 * @return
	 */
	public AmbiguityJob createAmbiguityJob(IProject project, ILanguageImpl language, Config config);
	
	/**
	 * Create a soundness job.
	 * 
	 * @param project
	 * @param language
	 * @param config
	 * @param interpreter
	 * @param timeout
	 * @return
	 */
	public SoundnessJob createSoundnessJob(IProject project, ILanguageImpl language, Config config, String interpreter, int timeout);
}
