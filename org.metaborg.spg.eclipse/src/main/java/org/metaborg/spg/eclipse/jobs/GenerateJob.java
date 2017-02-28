package org.metaborg.spg.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.Generator;
import org.metaborg.spg.eclipse.Activator;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;

import rx.Observable;

public class GenerateJob extends Job {
    protected MessageConsole console = ConsoleUtils.get("Spoofax console");
    protected MessageConsoleStream stream = console.newMessageStream();
    
    protected Generator generator;
    
	protected IProject project;
	protected ILanguageImpl language;
	protected int termLimit;
	protected int termSize;
	protected int fuel;
    
	public GenerateJob(Generator generator, IProject project, ILanguageImpl language, int termLimit, int termSize, int fuel) {
		super("Generate");
		
		this.generator = generator;
		
		this.project = project;
		this.language = language;
		this.termLimit = termLimit;
		this.termSize = termSize;
		this.fuel = fuel;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, termLimit);

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
        
        return Status.OK_STATUS;
    };
}
