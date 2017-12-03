package org.metaborg.spg.sentence.eclipse.handler;

import com.google.common.collect.Iterables;
import com.google.inject.Injector;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.metaborg.core.config.ConfigRequest;
import org.metaborg.core.config.ILanguageComponentConfig;
import org.metaborg.core.config.ILanguageComponentConfigService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spg.sentence.eclipse.Activator;
import org.metaborg.spg.sentence.eclipse.SentenceEclipseModule;
import org.metaborg.spg.sentence.eclipse.exception.LanguageNotFoundException;
import org.metaborg.spg.sentence.eclipse.exception.ProjectNotFoundException;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;

public abstract class SentenceHandler extends AbstractHandler {
    public static final Spoofax spoofax = SpoofaxPlugin.spoofax();
    public static final Injector injector = spoofax.injector.createChildInjector(new SentenceEclipseModule());

    protected IProject getProject(ExecutionEvent event) throws ProjectNotFoundException, ExecutionException {
        FileObject projectFile = getProjectFile(event);

        return injector
                .getInstance(IProjectService.class)
                .get(projectFile);
    }

    protected FileObject getProjectFile(ExecutionEvent event) throws ProjectNotFoundException, ExecutionException {
        FileObject selectionProjectFile = getProjectFile(HandlerUtil.getCurrentSelectionChecked(event));

        if (selectionProjectFile != null) {
            return selectionProjectFile;
        }

        FileObject workBenchProjectFile = getProjectFile(HandlerUtil.getActiveEditorInput(event));

        if (workBenchProjectFile != null) {
            return workBenchProjectFile;
        }

        throw new ProjectNotFoundException("Cannot find a project for generation.");
    }

    protected FileObject getProjectFile(ISelection selection) {
        if (selection instanceof ITreeSelection) {
            ITreeSelection treeSelection = (ITreeSelection) selection;

            for (TreePath treePath : treeSelection.getPaths()) {
                return getProjectFile(treePath);
            }
        }

        return null;
    }

    protected FileObject getProjectFile(IEditorInput editorInput) {
        if (editorInput != null) {
            return injector
                    .getInstance(IEclipseResourceService.class)
                    .resolve(editorInput);
        }

        return null;
    }

    protected FileObject getProjectFile(TreePath treePath) {
        IResource resource = getResource(treePath.getLastSegment());

        if (resource == null) {
            return null;
        }

        return injector
                .getInstance(IEclipseResourceService.class)
                .resolve(resource);
    }

    protected IResource getResource(Object selectedObject) {
        if (selectedObject instanceof IResource) {
            return (IResource) selectedObject;
        } else if (selectedObject instanceof IJavaProject) {
            return ((IJavaProject) selectedObject).getProject();
        }

        return null;
    }

    protected ILanguageImpl getLanguageImpl(IProject project) throws LanguageNotFoundException {
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
}
