package org.metaborg.spg.sentence.eclipse.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.eclipse.dialog.DifferenceDialog;
import org.metaborg.spg.sentence.eclipse.exception.LanguageNotFoundException;
import org.metaborg.spg.sentence.eclipse.exception.ProjectNotFoundException;
import org.metaborg.spg.sentence.eclipse.job.JobFactory;

public class LiberalDifferenceHandler extends SentenceHandler {
    public Object execute(ExecutionEvent executionEvent) throws ExecutionException {
        try {
            IProject project = getProject(executionEvent);
            ILanguageImpl languageImpl = getLanguageImpl(project);

            DifferenceDialog differenceDialog = new DifferenceDialog(getShell(executionEvent));
            differenceDialog.create();

            if (differenceDialog.open() == Window.OK) {
                JobFactory jobFactory = injector.getInstance(JobFactory.class);

                Job job = jobFactory.createLiberalDifferenceJob(project, languageImpl);
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
}
