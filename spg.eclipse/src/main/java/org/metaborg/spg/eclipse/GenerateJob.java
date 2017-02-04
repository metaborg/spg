package org.metaborg.spg.eclipse;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.config.ConfigRequest;
import org.metaborg.core.config.ILanguageComponentConfig;
import org.metaborg.core.config.ILanguageComponentConfigService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.Generator;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import rx.Observable;
import rx.functions.Action1;

public class GenerateJob extends Job {
	public static String SEMANTICS_PATH = "trans/static-semantics.nabl2";
	public static int TERM_LIMIT = 100;
	public static int FUEL = 500;
	public static int TERM_SIZE = 100;
	
    protected MessageConsole console = ConsoleUtils.get("Spoofax console");
    protected MessageConsoleStream stream = console.newMessageStream();
    
    protected IResourceService resourceService;
    protected IProjectService projectService;
    protected ILanguageService languageService;
    protected ILanguageComponentConfigService configService;
    protected Generator generator;
    
	protected FileObject project;
    
	@Inject
	public GenerateJob(IResourceService resourceService, IProjectService projectService, ILanguageService languageService, ILanguageComponentConfigService configService, Generator generator) {
		super("Generate");
		
		this.resourceService = resourceService;
		this.projectService = projectService;
		this.languageService = languageService;
		this.configService = configService;
		this.generator = generator;
	}
	
	/**
	 * Set the project that we will perform generation for.
	 * 
	 * @param project
	 */
	public void setProject(FileObject project) {
		this.project = project;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, TERM_LIMIT);
		
		IProject project = projectService.get(this.project);

		try {
			ILanguageImpl language = getLanguage(project);
			
			Config config = new Config(SEMANTICS_PATH, TERM_LIMIT, FUEL, TERM_SIZE, true, true);
			
			Observable<? extends String> programs = generator
				.generate(language, project, config)
				.asJavaObservable();
			
			programs.subscribe(new Action1<String>() {
				@Override
				public void call(String program) {
					stream.println(program);
					stream.println("--------------------------------------------");
					
					subMonitor.split(1);
				}
			}, new Action1<Throwable>() {
				@Override
				public void call(Throwable e) {
					if (e instanceof OperationCanceledException) {
						// Swallow cancellation exceptions
					} else {
						Activator.logError("An error occurred while generating terms.", e);
					}
				}
			});
		} catch (ProjectNotFoundException e) {
			Activator.logError("An error occurred while retrieving the language.", e);
		}
        
        return Status.OK_STATUS;
    };

    /**
     * Get language for given project.
     * 
     * @param path
     * @return
     * @throws ProjectNotFoundException 
     * @throws MetaborgException
     */
    protected ILanguageImpl getLanguage(IProject project) throws ProjectNotFoundException {
		ConfigRequest<ILanguageComponentConfig> projectConfig = configService.get(project.location());
		
		if (Iterables.size(projectConfig.errors()) != 0) {
			throw new ProjectNotFoundException("Unable to find config at " + project.location());
		}
		
		ILanguageComponentConfig config = projectConfig.config();
		
		if (config == null) {
			throw new ProjectNotFoundException("Unable to find config at " + project.location());
		}
		
		ILanguageImpl languageImpl = languageService.getImpl(config.identifier());
		
		if (languageImpl == null) {
			throw new ProjectNotFoundException("Unable to get implementation for language with identifier " + config.identifier());
		}
    	
    	return languageImpl;
    }
}
