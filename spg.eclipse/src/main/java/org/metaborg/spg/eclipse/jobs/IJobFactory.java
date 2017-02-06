package org.metaborg.spg.eclipse.jobs;

import org.apache.commons.vfs2.FileObject;

public interface IJobFactory {
	/**
	 * Create a genearte job.
	 * 
	 * @param project
	 * @param termLimit
	 * @param termSize
	 * @param fuel
	 * @param timeout
	 * @return
	 */
	public GenerateJob createGenerateJob(FileObject project, int termLimit, int termSize, int fuel);
	
	/**
	 * Create a soundness job.
	 * 
	 * @param project
	 * @param termLimit
	 * @param termSize
	 * @param fuel
	 * @param store
	 * @param interpreter
	 * @param timeout
	 * @return
	 */
	public SoundnessJob createSoundnessJob(FileObject project, int termLimit, int termSize, int fuel, boolean store, String interpreter, int timeout);
}
