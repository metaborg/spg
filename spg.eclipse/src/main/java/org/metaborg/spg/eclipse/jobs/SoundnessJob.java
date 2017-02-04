package org.metaborg.spg.eclipse.jobs;

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
import org.metaborg.core.language.ResourceExtensionFacet;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.Generator;
import org.metaborg.spg.eclipse.Activator;
import org.metaborg.spg.eclipse.ProjectNotFoundException;
import org.metaborg.spg.eclipse.models.ProcessOutput;
import org.metaborg.spg.eclipse.models.TerminateOutput;
import org.metaborg.spg.eclipse.models.TimeoutOutput;
import org.metaborg.spg.eclipse.rx.MapWithIndex;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import rx.Observable;

public class SoundnessJob extends GenerateJob {
	public static int TIMEOUT = 5;
	public static String CLIENT = "/Users/martijn/Projects/scopes-frames/L1.interpreter/L1-client";
	public static Charset UTF_8 = StandardCharsets.UTF_8;
	
	protected Integer termLimit;

	@Inject
	public SoundnessJob(IResourceService resourceService, IProjectService projectService, ILanguageService languageService, ILanguageComponentConfigService configService, Generator generator) {
		super(resourceService, projectService, languageService, configService, generator);
	}
	
	public void setTermLimit(Integer termLimit) {
		this.termLimit = termLimit;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, termLimit);
		final IProject project = projectService.get(this.project);
		
		// TODO: Start server for interpreter
		
		try {
			ILanguageImpl language = getLanguage(project);
			String extension = getExtension(language);
			
			Config config = new Config(SEMANTICS_PATH, termLimit, FUEL, TERM_SIZE, true, true);
			
			Observable<? extends String> programs = generator
				.generate(language, project, config)
				.asJavaObservable();
			
			Observable<ProcessOutput> outputs = programs.compose(MapWithIndex.<String>instance()).map(indexed -> {
				stream.println(indexed.value());
				stream.println("--------------------------------------------");
				
				try {
					String fileName = getName(indexed.index(), extension);
					FileObject programFile = storeProgram(indexed.value(), fileName);
					
					ProcessOutput processOutput = run(resourceService.localFile(programFile).getAbsolutePath());
					
					stream.println(processOutput.getOutput());
					stream.println(processOutput.getError());
					
					return processOutput;
				} catch (IOException | InterruptedException e) {
					Activator.logError("An error occurred while interpreting a generated term.", e);
					
					return new TerminateOutput("", "");
				}
			});
			
			Observable<ProcessOutput> finite = outputs.takeWhileWithIndex((output, index) -> {
				if (output.getError().contains("ReductionFailure") || output.getError().contains("IllegalStateException")) {
					stream.println("Found counterexample after " + (index + 1) + " terms.");
					
					return false;
				}
				
				return true;
			});
			
			finite.subscribe(output -> {
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
	}
	
	/**
	 * Compute the name for the program at given index and extension.
	 * 
	 * @param index
	 * @param extension
	 * @return
	 */
	protected String getName(long index, String extension) {
		return String.format("%05d", index) + "." + extension;
	}

	/**
	 * Get extension for given language implementation.
	 * 
	 * @param languageImpl
	 * @return
	 */
	protected String getExtension(ILanguageImpl languageImpl) {
		Iterable<String> extensions = languageImpl
			.facet(ResourceExtensionFacet.class)
			.extensions();
		
		if (Iterables.isEmpty(extensions)) {
			throw new RuntimeException("The language does not have any extensions.");
		}
		
		return Iterables.getFirst(extensions, null);
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
	    	process.exitValue();
	    	
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
