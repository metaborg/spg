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
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.eclipse.LanguageNotFoundException;
import org.metaborg.spg.eclipse.ProjectNotFoundException;
import org.metaborg.spg.eclipse.dialogs.SoundnessDialog;
import org.metaborg.spg.core.Config;
import org.metaborg.spg.eclipse.jobs.IJobFactory;

public class SoundnessHandler extends SpgHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			IProject project = getProject(event);
			ILanguageImpl language = getLanguage(project);
			
			SoundnessDialog soundnessDialog = new SoundnessDialog(getShell(event), getInterpreter(project));
			soundnessDialog.create();
			
			if (soundnessDialog.open() == Window.OK) {
				IJobFactory jobFactory = injector.getInstance(IJobFactory.class);
				
				Job job = jobFactory.createSoundnessJob(
					project,
					language,
					getConfig(soundnessDialog),
					soundnessDialog.getInterpreter(),
					soundnessDialog.getTimeout()
				);
				
				job.setPriority(Job.SHORT);
				job.setUser(true);
				job.schedule();
			}
		} catch (ProjectNotFoundException e) {
			MessageDialog.openError(null, "Project not found", "Cannot find a Spoofax project for generation.");
		} catch (LanguageNotFoundException e) {
			MessageDialog.openError(null, "Language not found", "Cannot find a Spoofax language for generation. Did you build the project?");
		} catch (FileSystemException e) {
			MessageDialog.openError(null, "Interpreter not found", "Cannot load the dynsem.properties file.");
		} catch (IOException e) {
			MessageDialog.openError(null, "Interpreter not found", "Cannot find the interpreter.");
		}
		
		return null;
	}
	
	/**
	 * Get a canonical path to the interpreter for the given project.
	 * 
	 * This implementation locates the dynsem.properties file in the language
	 * project to get the path to the interpreter project and constructs a path
	 * to the Nailgun client.
	 * 
	 * @param project
	 * @return
	 * @throws IOException 
	 * @throws FileSystemException 
	 */
	protected String getInterpreter(IProject project) throws FileSystemException, IOException {
		FileObject location = project.location();
        FileObject propertiesFile = spoofax.resourceService.resolve(location, "dynsem.properties");

        Properties properties = new Properties();
        properties.load(propertiesFile.getContent().getInputStream());

        String relInterpreterPath = properties.getProperty("project.path");
        String interpreterName = properties.getProperty("source.langname");

        Path localProjectPath = spoofax.resourceService.localFile(location).toPath();
        Path localInterpreterPath = localProjectPath.resolve(relInterpreterPath);

        return localInterpreterPath
    		.resolve(interpreterName + "-client")
    		.toFile()
    		.getCanonicalPath()
    		.toString();
	}
	
	protected Config getConfig(SoundnessDialog soundnessDialog) {
		return new Config(
			soundnessDialog.getTermLimit(),
			soundnessDialog.getFuel(),
			soundnessDialog.getTermSize(),
			true,
			true
		);
	}
}
