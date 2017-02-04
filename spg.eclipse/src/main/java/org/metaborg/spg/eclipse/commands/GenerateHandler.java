package org.metaborg.spg.eclipse.commands;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.metaborg.spg.eclipse.ProjectNotFoundException;
import org.metaborg.spg.eclipse.jobs.GenerateJob;

public class GenerateHandler extends SpgHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			FileObject project = getProject(event);
			
	        GenerateJob generateJob = spoofax.injector.getInstance(GenerateJob.class);
	        generateJob.setProject(project);
	        generateJob.setPriority(Job.SHORT);
	        generateJob.setUser(true);
	        generateJob.schedule();
		} catch (ProjectNotFoundException e) {
			MessageDialog.openError(null, "Project not found", "Cannot find a Spoofax project for generation.");
		}
		
		return null;
	}
}
