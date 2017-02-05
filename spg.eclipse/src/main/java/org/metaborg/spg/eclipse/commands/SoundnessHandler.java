package org.metaborg.spg.eclipse.commands;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.metaborg.spg.eclipse.ProjectNotFoundException;
import org.metaborg.spg.eclipse.dialogs.SoundnessDialog;
import org.metaborg.spg.eclipse.jobs.IJobFactory;

public class SoundnessHandler extends SpgHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			FileObject project = getProject(event);
			
			SoundnessDialog soundnessDialog = new SoundnessDialog(getShell(event));
			soundnessDialog.create();
			
			if (soundnessDialog.open() == Window.OK) {
				IJobFactory jobFactory = injector.getInstance(IJobFactory.class);
				
				Job job = jobFactory.createSoundnessJob(
					project,
					soundnessDialog.getTermLimit(),
					soundnessDialog.getTermSize(),
					soundnessDialog.getFuel(),
					soundnessDialog.getTimeout()
				);
				
				job.setPriority(Job.SHORT);
				job.setUser(true);
				job.schedule();
			}
		} catch (ProjectNotFoundException e) {
			MessageDialog.openError(null, "Project not found", "Cannot find a Spoofax project for generation.");
		}
		
		return null;
	}
}
