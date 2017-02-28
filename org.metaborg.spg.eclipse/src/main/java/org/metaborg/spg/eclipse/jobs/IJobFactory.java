package org.metaborg.spg.eclipse.jobs;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;

public interface IJobFactory {
	/**
	 * Create a genearte job.
	 * 
	 * @param project
	 * @param language
	 * @param termLimit
	 * @param termSize
	 * @param fuel
	 * @return
	 */
	public GenerateJob createGenerateJob(IProject project, ILanguageImpl language, int termLimit, int termSize, int fuel);
	
	/**
	 * Create an ambiguity job.
	 * 
	 * @param project
	 * @param language
	 * @param fuel
	 * @return
	 */
	public AmbiguityJob createAmbiguityJob(IProject project, ILanguageImpl language, int termLimit, int fuel);
	
	/**
	 * Create a soundness job.
	 * 
	 * @param project
	 * @param language
	 * @param termLimit
	 * @param termSize
	 * @param fuel
	 * @param interpreter
	 * @param timeout
	 * @return
	 */
	public SoundnessJob createSoundnessJob(IProject project, ILanguageImpl language, int termLimit, int termSize, int fuel, String interpreter, int timeout);
}
