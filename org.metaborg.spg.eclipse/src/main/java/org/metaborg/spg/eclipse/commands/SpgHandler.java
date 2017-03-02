package org.metaborg.spg.eclipse.commands;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.config.ConfigRequest;
import org.metaborg.core.config.ILanguageComponentConfig;
import org.metaborg.core.config.ILanguageComponentConfigService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spg.eclipse.Activator;
import org.metaborg.spg.eclipse.LanguageNotFoundException;
import org.metaborg.spg.eclipse.ProjectNotFoundException;
import org.metaborg.spg.eclipse.SpgEclipseModule;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;

import com.google.common.collect.Iterables;
import com.google.inject.Injector;

public abstract class SpgHandler extends AbstractHandler {
	public Spoofax spoofax;
	public Injector injector;
	
	public SpgHandler() {
		this.spoofax = SpoofaxPlugin.spoofax();
		this.injector = spoofax.injector.createChildInjector(new SpgEclipseModule());
	}
	
	/**
	 * Get a project based on the execution event.
	 * 
	 * @param event
	 * @return
	 * @throws ExecutionException
	 */
	protected IProject getProject(ExecutionEvent event) throws ProjectNotFoundException, ExecutionException {
		return spoofax.injector
			.getInstance(IProjectService.class)
			.get(getProjectFile(event));
	}
	
	/**
	 * Get a file object for the project based on the execution event.
	 * 
	 * @param event
	 * @return
	 * @throws ProjectNotFoundException
	 * @throws ExecutionException
	 */
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

	/**
	 * Get a file object based on a path in the tree view.
	 * 
	 * @param treePath
	 * @return
	 * @throws ProjectNotFoundException
	 */
	protected FileObject getProject(TreePath treePath) throws ProjectNotFoundException {
		Object selectedObject = treePath.getLastSegment();

		if (!(selectedObject instanceof IResource)) {
			throw new ProjectNotFoundException("Selected object is not an Eclipse resource.");
		}

		return spoofax.injector
			.getInstance(IEclipseResourceService.class)
			.resolve((IResource) selectedObject);
	}
	
    /**
     * Get language for given project.
     * 
     * @param path
     * @return
     * @throws ProjectNotFoundException 
     * @throws MetaborgException
     */
    protected ILanguageImpl getLanguage(IProject project) throws LanguageNotFoundException {
		ConfigRequest<ILanguageComponentConfig> projectConfig = spoofax.injector
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
		
		ILanguageImpl languageImpl = spoofax.injector
			.getInstance(ILanguageService.class)
			.getImpl(config.identifier());
		
		if (languageImpl == null) {
			throw new LanguageNotFoundException("Unable to get implementation for language with identifier " + config.identifier());
		}
    	
    	return languageImpl;
    }
	
	/**
	 * Get the shell for the given execution event.
	 * 
	 * @param event
	 * @return
	 */
	protected Shell getShell(ExecutionEvent event) {
		return HandlerUtil.getActiveShell(event);
	}
}
