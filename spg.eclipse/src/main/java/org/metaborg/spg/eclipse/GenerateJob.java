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
import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.GeneratorEntryPoint;
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
		Config config = new Config(TERM_LIMIT, 500, 100, true, true);

		Observable<? extends String> programs = new GeneratorEntryPoint()
			.generate(
				"zip:/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0.spoofax-language!/",
				"zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0.spoofax-language!/",
				spoofax.resourceService.localPath(project).toString(),
				"trans/static-semantics.nabl2",
				config
			)
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
}
