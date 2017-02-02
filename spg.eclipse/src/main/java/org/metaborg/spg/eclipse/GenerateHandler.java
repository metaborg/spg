package org.metaborg.spg.eclipse;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.handlers.HandlerUtil;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;

public class GenerateHandler extends AbstractHandler {
	public Spoofax spoofax = SpoofaxPlugin.spoofax();	
    
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			FileObject project = getProject(event);
			
	        Activator.logInfo("Run SPG on project " + project);
	        
			//ILanguageImpl nablLanguage = getLanguage("org.metaborg:org.metaborg.meta.nabl2.lang:2.1.0");
			
	        Job job = new GenerateJob(spoofax, project);
	        job.setPriority(Job.SHORT);
	        job.setUser(true);
	        job.schedule();
		} catch (ProjectNotFoundException e) {
			MessageDialog.openError(null, "Project not found", "Cannot find a Spoofax project for generation.");
		}
		
		return null;
	}
	
	/**
	 * Get a project based on the execution event.
	 * 
	 * @param event
	 * @return
	 * @throws ExecutionException
	 */
	private FileObject getProject(ExecutionEvent event) throws ProjectNotFoundException, ExecutionException {
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
	 * Get a project based on a path in the tree view.
	 * 
	 * @param treePath
	 * @return
	 * @throws ProjectNotFoundException
	 */
	private FileObject getProject(TreePath treePath) throws ProjectNotFoundException {
		Object selectedObject = treePath.getLastSegment();

		if (!(selectedObject instanceof IResource)) {
			Activator.logWarn("Project of type " + selectedObject.getClass() + " not recognized.");
			
			throw new ProjectNotFoundException("Selected object is not an Eclipse resource.");
		}

		return spoofax.injector
			.getInstance(IEclipseResourceService.class)
			.resolve((IResource) selectedObject);
	}
	
	/**
	 * Get a language implementation based on its identifier.
	 * 
	 * @param identifier
	 * @return
	 */
	protected ILanguageImpl getLanguage(String identifier) {
		LanguageIdentifier languageIdentifier = LanguageIdentifier.parse(identifier);
		
		return spoofax.languageService.getImpl(languageIdentifier);
	}
}
