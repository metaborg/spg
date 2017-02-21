package org.metaborg.spg.eclipse.jobs;

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
import org.metaborg.core.messages.IMessage;
import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.Generator;
import org.metaborg.spg.eclipse.Activator;
import org.metaborg.spg.eclipse.ProjectNotFoundException;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;

import com.google.common.collect.Iterables;

import rx.Observable;

public class GenerateJob extends Job {
    protected MessageConsole console = ConsoleUtils.get("Spoofax console");
    protected MessageConsoleStream stream = console.newMessageStream();
    
    protected IResourceService resourceService;
    protected IProjectService projectService;
    protected ILanguageService languageService;
    protected ILanguageComponentConfigService configService;
    protected Generator generator;
    
	protected FileObject project;
	protected int termLimit;
	protected int termSize;
	protected int fuel;
	protected int timeout;
    
	public GenerateJob(IResourceService resourceService, IProjectService projectService, ILanguageService languageService, ILanguageComponentConfigService configService, Generator generator, FileObject project, int termLimit, int termSize, int fuel) {
		super("Generate");
		
		this.resourceService = resourceService;
		this.projectService = projectService;
		this.languageService = languageService;
		this.configService = configService;
		this.generator = generator;
		
		this.project = project;
		this.termLimit = termLimit;
		this.termSize = termSize;
		this.fuel = fuel;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, termLimit);
		final IProject project = projectService.get(this.project);

		try {
			ILanguageImpl language = getLanguage(project);
			
			Config config = new Config(termLimit, fuel, termSize, true, true);
			
			Observable<? extends String> programs = generator
				.generate(language, project, config)
				.asJavaObservable();
			
			programs.subscribe(program -> {
				stream.println(program);
				stream.println("--------------------------------------------");
				
				subMonitor.split(1);
			}, exception -> {
				if (exception instanceof OperationCanceledException) {
					// Swallow cancellation exceptions
				} else {
					Activator.logError("An error occurred while generating terms.", exception);
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
			for (IMessage message : projectConfig.errors()) {
				Activator.logError(message.message(), message.exception());
			}
			
			throw new ProjectNotFoundException("One or more errors occurred while retrieving config at " + project.location());
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
