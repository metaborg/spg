package org.metaborg.spg.eclipse;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.handlers.HandlerUtil;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;

import rx.functions.Action1;

import org.metaborg.spg.GeneratorEntryPoint;

public class GenerateHandler extends AbstractHandler {
	private IEclipseResourceService resourceService = SpoofaxPlugin.injector().getInstance(IEclipseResourceService.class);
	
    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        FileObject project = getSelection(event);
        
        Activator.logInfo("Run SPG on project " + project);
        
        new GeneratorEntryPoint().generate("", "", "", "", new Config(1, 2, 3)).subscribe(
    		new Action1<String>() {
    			@Override
    			public void call(String term) {
    				System.out.println(term);
    			}
    		}
		);
        
    	return null;
    }

    /**
     * Get a Spoofax project based on the event.
     * 
     * @param event
     * @return 
     * @throws ExecutionException 
     */
	private FileObject getSelection(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
        
        if (selection instanceof ITreeSelection) {
            ITreeSelection treeSelection = (ITreeSelection) selection;
            
            for (TreePath treePath : treeSelection.getPaths()) {
            	 return getProject(treePath);
            }
        }
        
        throw new ExecutionException("Cannot find a project for generation.");
	}

	/**
	 * Get a project based on a path in the tree view.
	 *  
	 * @param treePath
	 * @return 
	 */
	private FileObject getProject(TreePath treePath) {
		Object selectedObject = treePath.getLastSegment();

		if (!(selectedObject instanceof IResource)) {
			Activator.logWarn("Project of type " + selectedObject.getClass() + " not recognized.");
		}
		
		return resourceService.resolve((IResource) selectedObject);
	}
}
