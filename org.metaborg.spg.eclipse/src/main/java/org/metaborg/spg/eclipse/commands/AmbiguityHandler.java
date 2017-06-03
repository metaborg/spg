package org.metaborg.spg.eclipse.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.eclipse.LanguageNotFoundException;
import org.metaborg.spg.eclipse.ProjectNotFoundException;
import org.metaborg.spg.eclipse.dialogs.AmbiguityDialog;
import org.metaborg.spg.core.Config;
import org.metaborg.spg.eclipse.jobs.IJobFactory;

public class AmbiguityHandler extends SpgHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			IProject project = getProject(event);
			ILanguageImpl language = getLanguage(project);
			
			AmbiguityDialog ambiguityDialog = new AmbiguityDialog(getShell(event));
			ambiguityDialog.create();
			
			if (ambiguityDialog.open() == Window.OK) {
				IJobFactory jobFactory = injector.getInstance(IJobFactory.class);
				
				Job job = jobFactory.createAmbiguityJob(
					project,
					language,
					getConfig(ambiguityDialog)
				);
				
				job.setPriority(Job.SHORT);
				job.setUser(true);
				job.schedule();
			}
		} catch (ProjectNotFoundException e) {
			MessageDialog.openError(null, "Project not found", "Cannot find a Spoofax project for generation. Did you select it in the Package Explorer?");
		} catch (LanguageNotFoundException e) {
			MessageDialog.openError(null, "Language not found", "Cannot find a Spoofax language for generation. Did you build the project?");
		}
		
		return null;
	}
	
	protected Config getConfig(AmbiguityDialog ambiguityDialog) {
		return new Config(
			ambiguityDialog.getTermLimit(),
			0,
			ambiguityDialog.getTermSize()
		);
	}
}
