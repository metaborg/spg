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
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.Generator;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;

import rx.Observable;
import rx.functions.Action1;

public class GenerateJob extends Job {
	public static int TERM_LIMIT = 100;
	
    protected MessageConsole console = ConsoleUtils.get("Spoofax console");
    protected MessageConsoleStream stream = console.newMessageStream();
    protected Spoofax spoofax;
	protected FileObject project;
    
	public GenerateJob(Spoofax spoofax, FileObject project) {
		super("Generate");
		
		this.spoofax = spoofax;
		this.project = project;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		Activator.logInfo("Local path: " + spoofax.resourceService.localPath(project));
		
		final SubMonitor subMonitor = SubMonitor.convert(monitor, TERM_LIMIT);
		
		// Generate infinite terms (10) with fuel (500) and limit size (100). Make these configurable with a dialog?
		Config config = new Config("trans/static-semantics.nabl2", TERM_LIMIT, 500, 100, true, true);
		
		Generator generator = spoofax.injector.getInstance(Generator.class);

		IProject project = spoofax.projectService.get(this.project);
		ILanguageImpl language = getLanguage(project);
		
		Observable<? extends String> programs = generator
			.generate(language, project, config)
			.asJavaObservable();
		
		programs.subscribe(new Action1<String>() {
			@Override
			public void call(String program) {
				// Print to Spoofax console?
				stream.println(program);
				
				// Log in OSGi?
				Activator.logInfo(program);
				
				// Report progress
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
        
        return Status.OK_STATUS;
    };

    /**
     * Get language for given project.
     * 
     * @param path
     * @return
     * @throws MetaborgException
     */
    protected ILanguageImpl getLanguage(IProject project) {
		ILanguageComponentConfigService configService = spoofax.injector.getInstance(ILanguageComponentConfigService.class);
		ConfigRequest<ILanguageComponentConfig> projectConfig = configService.get(project.location());
		LanguageIdentifier identifier = projectConfig.config().identifier();
		ILanguageImpl languageImpl = spoofax.languageService.getImpl(identifier);
    	
    	return languageImpl;
    }
}
