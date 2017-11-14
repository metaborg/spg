package org.metaborg.spg.sentence.eclipse.handler;

import com.google.common.collect.Iterables;
import com.google.inject.Injector;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.metaborg.core.config.ConfigRequest;
import org.metaborg.core.config.ILanguageComponentConfig;
import org.metaborg.core.config.ILanguageComponentConfigService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spg.sentence.ambiguity.AmbiguityTesterConfig;
import org.metaborg.spg.sentence.eclipse.Activator;
import org.metaborg.spg.sentence.eclipse.SentenceEclipseModule;
import org.metaborg.spg.sentence.eclipse.dialog.SentenceDialog;
import org.metaborg.spg.sentence.eclipse.exception.LanguageNotFoundException;
import org.metaborg.spg.sentence.eclipse.exception.ProjectNotFoundException;
import org.metaborg.spg.sentence.eclipse.job.SentenceJobFactory;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;

public class SentenceHandler extends AbstractHandler {
    private final Injector injector;

    public SentenceHandler() {
        this.injector = SpoofaxPlugin.spoofax().injector.createChildInjector(new SentenceEclipseModule());
    }

    public Object execute(ExecutionEvent executionEvent) throws ExecutionException {
        try {
            IProject project = getProject(executionEvent);
            ILanguageImpl language = getLanguage(project);

            SentenceDialog sentenceDialog = new SentenceDialog(getShell(executionEvent));
            sentenceDialog.create();

            if (sentenceDialog.open() == Window.OK) {
                SentenceJobFactory jobFactory = injector.getInstance(SentenceJobFactory.class);
                Job job = jobFactory.createSentenceJob(project, language, getConfig(sentenceDialog));

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

    protected IProject getProject(ExecutionEvent event) throws ProjectNotFoundException, ExecutionException {
        return injector
                .getInstance(IProjectService.class)
                .get(getProjectFile(event));
    }

    protected FileObject getProjectFile(ExecutionEvent event) throws ProjectNotFoundException, ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

        if (selection instanceof ITreeSelection) {
            ITreeSelection treeSelection = (ITreeSelection) selection;

            for (TreePath treePath : treeSelection.getPaths()) {
                return getProject(treePath);
            }
        }

        throw new ProjectNotFoundException("Cannot find a project for generation.");
    }

    protected FileObject getProject(TreePath treePath) throws ProjectNotFoundException {
        Object selectedObject = treePath.getLastSegment();

        return injector
                .getInstance(IEclipseResourceService.class)
                .resolve(getResource(selectedObject));
    }

    protected IResource getResource(Object selectedObject) throws ProjectNotFoundException {
        if (selectedObject instanceof IResource) {
            return (IResource) selectedObject;
        } else if (selectedObject instanceof IJavaProject) {
            return ((IJavaProject) selectedObject).getProject();
        }

        throw new ProjectNotFoundException("Selected object is not an IResource or IJavaProject.");
    }

    protected ILanguageImpl getLanguage(IProject project) throws LanguageNotFoundException {
        ConfigRequest<ILanguageComponentConfig> projectConfig = injector
                .getInstance(ILanguageComponentConfigService.class)
                .get(project.location());

        if (Iterables.size(projectConfig.errors()) != 0) {
            for (IMessage message : projectConfig.errors()) {
                Activator.logError(message.message(), message.exception());
            }

            throw new LanguageNotFoundException("One or more errors occurred while retrieving config at " + project.location());
        }

        ILanguageComponentConfig config = projectConfig.config();

        if (config == null) {
            throw new LanguageNotFoundException("Unable to find config at " + project.location());
        }

        ILanguageImpl languageImpl = injector
                .getInstance(ILanguageService.class)
                .getImpl(config.identifier());

        if (languageImpl == null) {
            throw new LanguageNotFoundException("Unable to get implementation for language with identifier " + config.identifier());
        }

        return languageImpl;
    }

    protected Shell getShell(ExecutionEvent event) {
        return HandlerUtil.getActiveShell(event);
    }

    protected AmbiguityTesterConfig getConfig(SentenceDialog generateDialog) {
        int maxNumberOfTerms = generateDialog.getTermLimit();
        int maxTermSize = generateDialog.getTermSize();

        return new AmbiguityTesterConfig(maxNumberOfTerms, maxTermSize);
    }
}
