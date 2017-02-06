package org.metaborg.spg.eclipse.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
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
			
			SoundnessDialog soundnessDialog = new SoundnessDialog(getShell(event), getInterpreter(project));
			soundnessDialog.create();
			
			if (soundnessDialog.open() == Window.OK) {
				IJobFactory jobFactory = injector.getInstance(IJobFactory.class);
				
				Job job = jobFactory.createSoundnessJob(
					project,
					soundnessDialog.getTermLimit(),
					soundnessDialog.getTermSize(),
					soundnessDialog.getFuel(),
					soundnessDialog.getStore(),
					soundnessDialog.getInterpreter(),
					soundnessDialog.getTimeout()
				);
				
				job.setPriority(Job.SHORT);
				job.setUser(true);
				job.schedule();
			}
		} catch (ProjectNotFoundException e) {
			MessageDialog.openError(null, "Project not found", "Cannot find a Spoofax project for generation.");
		} catch (FileSystemException e) {
			MessageDialog.openError(null, "Interpreter not found", "Cannot find the interpreter.");
		} catch (IOException e) {
			MessageDialog.openError(null, "Interpreter not found", "Cannot find the interpreter.");
		}
		
		return null;
	}
	
	/**
	 * Get the path to the interpreter for the given project.
	 * 
	 * @param project
	 * @return
	 * @throws IOException 
	 * @throws FileSystemException 
	 */
	protected String getInterpreter(FileObject project) throws FileSystemException, IOException {
        FileObject propertiesFile = spoofax.resourceService.resolve(project, "dynsem.properties");

        Properties properties = new Properties();
        properties.load(propertiesFile.getContent().getInputStream());

        String relInterpreterPath = properties.getProperty("project.path");
        String interpreterName = properties.getProperty("source.langname");

        Path localProjectPath = spoofax.resourceService.localFile(project).toPath();
        Path localInterpreterPath = localProjectPath.resolve(relInterpreterPath);

        return localInterpreterPath.resolve(interpreterName + "-client").toAbsolutePath().toString();
	}
}
