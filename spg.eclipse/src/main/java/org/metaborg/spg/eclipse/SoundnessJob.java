package org.metaborg.spg.eclipse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.metaborg.core.config.ILanguageComponentConfigService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.Generator;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.util.MapWithIndex.Indexed;
import com.google.inject.Inject;

import rx.Observable;
import rx.functions.Action1;

public class SoundnessJob extends GenerateJob {
	public static int TIMEOUT = 5;
	public static String CLIENT = "/Users/martijn/Projects/metaborg-tiger/org.metaborg.lang.tiger.interpreter/tiger-client";
	public static Charset UTF_8 = StandardCharsets.UTF_8;
	
	protected ISourceTextService textService;
	
	@Inject
	public SoundnessJob(IResourceService resourceService, IProjectService projectService, ILanguageService languageService, ILanguageComponentConfigService configService, ISourceTextService textService, Generator generator) {
		super(resourceService, projectService, languageService, configService, generator);
		
		this.textService = textService;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, TERM_LIMIT);
		
		final IProject project = projectService.get(this.project);

		// TODO: Start server for interpreter
		
		try {
			ILanguageImpl language = getLanguage(project);
			
			Config config = new Config(SEMANTICS_PATH, TERM_LIMIT, FUEL, TERM_SIZE, true, true);
			
			Observable<? extends String> programs = generator
				.generate(language, project, config)
				.asJavaObservable();
			
			Observable<Indexed<String>> indexedPrograms = programs
				.compose(Transformers.<String>mapWithIndex());
			
			indexedPrograms.subscribe(new Action1<Indexed<String>>() {
				@Override
				public void call(Indexed<String> indexed) {
					stream.println(indexed.value());
					stream.println("--------------------------------------------");
					
					try {
						String fileName = indexed.index() + ".tig";
						FileObject programFile = storeProgram(indexed.value(), fileName);
						
						ProcessOutput processOutput = run(resourceService.localFile(programFile).getAbsolutePath());
						
						if (processOutput.getError().contains("ReductionFailure")) {
							Activator.logInfo("Found a counterexample: " + indexed.value());
						}
						
						subMonitor.split(1);
					} catch (IOException | InterruptedException e) {
						Activator.logError("An error occurred while running a generated term.", e);
					}
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
	}
	
	/**
	 * Store the generated program on the filesystem.
	 * 
	 * @param program
	 * @throws IOException
	 */
	protected FileObject storeProgram(String program, String name) throws IOException {
		FileObject fileObject = resourceService.resolve(project, "1.tig");
		FileContent fileContent = fileObject.getContent();
		
		try (OutputStream outputStream = fileContent.getOutputStream()) {
			IOUtils.write(program, outputStream, UTF_8);
		}
	    
	    return fileObject;
	}

	/**
	 * Run the interpreter on the program at the given path and return the
	 * process output.
	 * 
	 * @param path
	 * @return 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	protected ProcessOutput run(String path) throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder(CLIENT, path);
	    Process process = processBuilder.start();

	    OutputStream outputStream = process.getOutputStream();
	    outputStream.write(randomString().getBytes(UTF_8));
	    outputStream.flush();

	    if (!process.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
	      process.destroyForcibly();

	      String out = IOUtils.toString(process.getInputStream(), UTF_8);
	      String err = IOUtils.toString(process.getErrorStream(), UTF_8);
	      
	      return new TimeoutOutput(out, err);
	    } else {
	      // For some reason, without this call waitFor sometimes hangs
	      int exitValue = process.exitValue();

	      String out = IOUtils.toString(process.getInputStream(), UTF_8);
	      String err = IOUtils.toString(process.getErrorStream(), UTF_8);

	      return new TerminateOutput(out, err);
	    }
	}
	
	/**
	 * Generate a random string.
	 * 
	 * @return
	 */
	protected String randomString() {
		return "abc";
	}
}
