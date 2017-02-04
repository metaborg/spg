package org.metaborg.spg.eclipse;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;

public class SoundnessHandler extends SpgHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			FileObject project = getProject(event);
			
	        SoundnessJob soundnessJob = spoofax.injector.getInstance(SoundnessJob.class);
	        soundnessJob.setProject(project);
	        soundnessJob.setPriority(Job.SHORT);
	        soundnessJob.setUser(true);
	        soundnessJob.schedule();
		} catch (ProjectNotFoundException e) {
			MessageDialog.openError(null, "Project not found", "Cannot find a Spoofax project for generation.");
		}
		
		return null;
	}
}
