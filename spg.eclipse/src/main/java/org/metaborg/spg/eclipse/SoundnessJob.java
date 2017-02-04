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
import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.Generator;
import org.metaborg.spg.eclipse.rx.MapWithIndex;
import org.metaborg.spg.eclipse.rx.MapWithIndex.Indexed;

import com.google.inject.Inject;

import rx.Observable;
import rx.functions.Action1;

public class SoundnessJob extends GenerateJob {
	public static int TIMEOUT = 5;
	public static String CLIENT = "/Users/martijn/Projects/scopes-frames/L1.interpreter/L1-client";
	public static Charset UTF_8 = StandardCharsets.UTF_8;

	@Inject
	public SoundnessJob(IResourceService resourceService, IProjectService projectService, ILanguageService languageService, ILanguageComponentConfigService configService, Generator generator) {
		super(resourceService, projectService, languageService, configService, generator);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, TERM_LIMIT);
		
		final IProject project = projectService.get(this.project);
		
		Activator.logInfo("RUn");

		// TODO: Start server for interpreter
		
		try {
			ILanguageImpl language = getLanguage(project);
			
			Config config = new Config(SEMANTICS_PATH, TERM_LIMIT, FUEL, TERM_SIZE, true, true);
			
			Observable<? extends String> programs = generator
				.generate(language, project, config)
				.asJavaObservable();
			
			Observable<Indexed<String>> indexedPrograms = programs
				.compose(MapWithIndex.<String>instance());
			
			// TODO: Can we use Java 8?
			indexedPrograms.subscribe(new Action1<Indexed<String>>() {
				@Override
				public void call(Indexed<String> indexed) {
					stream.println(indexed.value());
					stream.println("--------------------------------------------");
					
					try {
						// TODO: Get language extension
						String fileName = indexed.index() + ".l1";
						FileObject programFile = storeProgram(indexed.value(), fileName);
						
						ProcessOutput processOutput = run(resourceService.localFile(programFile).getAbsolutePath());
						
						stream.println(processOutput.getOutput());
						stream.println(processOutput.getError());
						
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
		FileObject fileObject = resourceService.resolve(project, name);
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
