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
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;

public class GenerateHandler extends AbstractHandler {
	public Spoofax spoofax = SpoofaxPlugin.spoofax();	
    
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			FileObject project = getProject(event);
			
	        GenerateJob generateJob = spoofax.injector.getInstance(GenerateJob.class);
	        generateJob.setProject(project);
	        generateJob.setPriority(Job.SHORT);
	        generateJob.setUser(true);
	        generateJob.schedule();
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
			throw new ProjectNotFoundException("Selected object is not an Eclipse resource.");
		}

		return spoofax.injector
			.getInstance(IEclipseResourceService.class)
			.resolve((IResource) selectedObject);
	}
}
