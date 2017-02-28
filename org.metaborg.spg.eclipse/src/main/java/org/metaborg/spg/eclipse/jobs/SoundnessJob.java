package org.metaborg.spg.eclipse.jobs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ResourceExtensionFacet;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.Generator;
import org.metaborg.spg.eclipse.Activator;
import org.metaborg.spg.eclipse.models.ProcessOutput;
import org.metaborg.spg.eclipse.models.TerminateOutput;
import org.metaborg.spg.eclipse.models.TimeoutOutput;
import org.metaborg.spg.eclipse.rx.MapWithIndex;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;

import com.google.common.collect.Iterables;

import rx.Observable;

public class SoundnessJob extends Job {
	public static Charset UTF_8 = StandardCharsets.UTF_8;
	
    protected MessageConsole console = ConsoleUtils.get("Spoofax console");
    protected MessageConsoleStream stream = console.newMessageStream();
    
    protected Generator generator;
    
	protected IProject project;
	protected ILanguageImpl language;
	protected int termLimit;
	protected int termSize;
	protected int fuel;
	protected String interpreter;
	protected int timeout;

	public SoundnessJob(Generator generator, IProject project, ILanguageImpl language, int termLimit, int termSize, int fuel, String interpreter, int timeout) {
		super("Soundness");
		
		this.generator = generator;
		
		this.project = project;
		this.language = language;
		this.termLimit = termLimit;
		this.termSize = termSize;
		this.fuel = fuel;
		this.interpreter = interpreter;
		this.timeout = timeout;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, termLimit);
		
		String extension = getExtension(language);
		long startTime = System.currentTimeMillis();
		
		Config config = new Config(termLimit, fuel, termSize, true, true);
		
		Observable<? extends String> programs = generator
			.generate(language, project, config)
			.asJavaObservable();
		
		programs
			.doOnNext(file -> subMonitor.split(1))
			.map(program -> store(program, extension))
			.map(file -> execute(file))
			.compose(MapWithIndex.instance())
			.skipWhile(indexedParseUnit -> !error(indexedParseUnit.value()))
			.first()
			.subscribe(indexedOutput -> {
				stream.println("Found counterexample after " + (indexedOutput.index() + 1) + " terms (" + (System.currentTimeMillis()-startTime)/1000 + " seconds).");
			}, exception -> {
				if (exception instanceof OperationCanceledException) {
					// Swallow cancellation exceptions
				} else {
					Activator.logError("An error occurred while generating terms.", exception);
				}
			}, () -> {
				subMonitor.setWorkRemaining(0);
				subMonitor.done();
			})
		;
		
		return Status.OK_STATUS;
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
	 * Store the generated program in a temporary file with the given
	 * extension.
	 * 
	 * We need to store files on the filesystem (as opposted to RAM or VFS),
	 * because the interpreter takes files from the filesystem.
	 * 
	 * @param program
	 * @throws IOException
	 */
	protected File store(String program, String extension) {
		try {
		    File file = File.createTempFile("spg", extension);
		    
		    try (FileWriter fileWriter = new FileWriter(file)) {
		    	fileWriter.write(program);
		    }
		    
		    return file;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Execute the program in the given file and return the process output.
	 * 
	 * @param path
	 * @return 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	protected ProcessOutput execute(File file) {
		try {
			return execute(file.getCanonicalPath());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Execute the program at the given path and return the process output.
	 * 
	 * @param path
	 * @return 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	protected ProcessOutput execute(String path) throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder(interpreter, path);
	    Process process = processBuilder.start();
	    
	    try (OutputStream outputStream = process.getOutputStream()) {
	    	outputStream.write(randomString().getBytes(UTF_8));
	    	outputStream.flush();
	    } catch (IOException e) {
	    	// Silently fail when we cannot write our random string..
	    }

	    if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
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
	 * Check if the output contains an error.
	 * 
	 * @param output
	 * @return
	 */
	protected boolean error(ProcessOutput output) {
		return Boolean.logicalOr(
			output.getError().contains("ReductionFailure"),
			output.getError().contains("IllegalStateException")
		);
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
