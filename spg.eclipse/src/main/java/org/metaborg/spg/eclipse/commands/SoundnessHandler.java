package org.metaborg.spg.eclipse.commands;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.metaborg.spg.eclipse.ProjectNotFoundException;
import org.metaborg.spg.eclipse.dialogs.GenerateDialog;
import org.metaborg.spg.eclipse.jobs.SoundnessJob;

public class SoundnessHandler extends SpgHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			FileObject project = getProject(event);
			
			Shell shell = HandlerUtil.getActiveShell(event);
			
			GenerateDialog generateDialog = new GenerateDialog(shell);
			generateDialog.create();
			
			if (generateDialog.open() == Window.OK) {
		        SoundnessJob soundnessJob = spoofax.injector.getInstance(SoundnessJob.class);
		        
		        // Generator-specific settings
		        soundnessJob.setProject(project);
		        soundnessJob.setTermLimit(generateDialog.getTermLimit());
		        
		        // General dialog settings
		        soundnessJob.setPriority(Job.SHORT);
		        soundnessJob.setUser(true);
		        
		        soundnessJob.schedule();
			}
		} catch (ProjectNotFoundException e) {
			MessageDialog.openError(null, "Project not found", "Cannot find a Spoofax project for generation.");
		}
		
		return null;
	}
}
