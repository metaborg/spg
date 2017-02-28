package org.metaborg.spg.eclipse.jobs;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
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
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.SyntaxGenerator;
import org.metaborg.spg.eclipse.Activator;
import org.metaborg.spg.eclipse.ProjectNotFoundException;
import org.metaborg.spg.eclipse.rx.MapWithIndex;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import rx.Observable;

public class AmbiguityJob extends Job {
	public static Charset UTF_8 = StandardCharsets.UTF_8;
	
    protected MessageConsole console = ConsoleUtils.get("Spoofax console");
    protected MessageConsoleStream stream = console.newMessageStream();
    
    protected IResourceService resourceService;
    protected IProjectService projectService;
    protected ILanguageService languageService;
    protected ILanguageComponentConfigService configService;
    protected ISourceTextService sourceTextService;
    protected ISpoofaxUnitService unitService;
    protected ISpoofaxSyntaxService syntaxService;
    protected SyntaxGenerator generator;
    
	protected FileObject project;
	protected int termLimit;
	protected int fuel;
	protected int timeout;
    
	public AmbiguityJob(IResourceService resourceService, IProjectService projectService, ILanguageService languageService, ILanguageComponentConfigService configService, ISourceTextService sourceTextService, ISpoofaxUnitService unitService, ISpoofaxSyntaxService syntaxService, SyntaxGenerator generator, FileObject project, int termLimit, int fuel) {
		super("Generate");
		
		this.resourceService = resourceService;
		this.projectService = projectService;
		this.languageService = languageService;
		this.configService = configService;
		this.sourceTextService = sourceTextService;
		this.unitService = unitService;
		this.syntaxService = syntaxService;
		
		this.generator = generator;
		
		this.project = project;
		this.termLimit = termLimit;
		this.fuel = fuel;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, termLimit);
		final IProject project = projectService.get(this.project);

		try {
			ILanguageImpl language = getLanguage(project);
			
			Config config = new Config(termLimit, fuel, 0, true, true);
			
			Observable<? extends String> programs = generator
				.generate(language, project, config)
				.asJavaObservable();
			
			programs
				.doOnNext(program -> progress(subMonitor, program))
				.map(program -> store(program))
				.map(file -> parse(language, file))
				.compose(MapWithIndex.instance())
				.skipWhile(indexedParseUnit -> !ambiguous(indexedParseUnit.value()))
				.first()
				.subscribe(indexedParseUnit -> {
					IStrategoTerm ast = parse(language, indexedParseUnit.value().input().source()).ast();
					
					stream.println("=== Ambiguities ===");
					stream.println(Joiner.on("\n").join(ambiguities(ast)));	           
				}, exception -> {
					if (exception instanceof OperationCanceledException) {
						// Swallow cancellation exceptions
					} else {
						Activator.logError("An error occurred while generating terms.", exception);
					}
				})
			;
		} catch (ProjectNotFoundException e) {
			Activator.logError("An error occurred while retrieving the language.", e);
		}
        
        return Status.OK_STATUS;
    };
    
    /**
     * Show there is some prorgress by printing the program and incrementing
	 * the submonitor.
     * 
     * @param program
     */
    protected void progress(SubMonitor monitor, String program) {
    	stream.println("=== Program ===");
    	stream.println(program);
		
    	monitor.split(1);
    }
    
    /**
     * Store the given program.
     * 
     * This implementation stores the program in RAM, using the RAM provider
     * from VFS.
     * 
     * @return
     * @throws IOException
     */
    protected FileObject store(String program) {
    	try {
	    	FileObject fileObject = resourceService.resolve("ram://" + System.nanoTime() + ".jav");
	    	fileObject.createFile();
	    	
	    	Writer writer = new PrintWriter(fileObject.getContent().getOutputStream());
	    	writer.write(program);
	    	writer.flush();
	    	
	    	return fileObject;
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * Parse the given file object in the given language.
     * 
     * @return
     * @throws IOException 
     * @throws ParseException 
     */
    protected ISpoofaxParseUnit parse(ILanguageImpl language, FileObject fileObject) {
    	try {
    		String text = sourceTextService.text(fileObject);
        	ISpoofaxInputUnit inputUnit = unitService.inputUnit(fileObject, text, language, null);
        	
			return syntaxService.parse(inputUnit);
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }

    /**
     * Read the program from a parse unit.
     * 
     * @param parseUnit
     * @return
     */
    protected String read(ISpoofaxParseUnit parseUnit) {
    	try {
    		FileObject fileObject = parseUnit.input().source();
    		InputStream inputStream = fileObject.getContent().getInputStream();
    		String program = IOUtils.toString(inputStream, UTF_8);
    		
    		return program;
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * Check if the parse unit contains an ambiguous AST.
     * 
     * @param parseUnit
     * @return
     */
    protected boolean ambiguous(ISpoofaxParseUnit parseUnit) {
    	return ambiguous(parseUnit.ast());
    }
    
    /**
     * Check if the AST contains the 'amb' constructor.
     * 
     * @param term
     * @return
     */
    protected boolean ambiguous(IStrategoTerm term) {
        return !ambiguities(term).isEmpty();
    }

    /**
      * Collect all 'amb' constructors.
      *
      * @param term
      * @return
      */
    protected List<IStrategoTerm> ambiguities(IStrategoTerm term) {
	    if (term instanceof IStrategoAppl) {
		    IStrategoAppl appl = (IStrategoAppl) term;
		    
		    if (appl.getConstructor().getName() == "amb") {
			    return Collections.singletonList(term);
		    }
	    }

	    return Arrays
    		.stream(term.getAllSubterms())
    		.flatMap(child -> ambiguities(child).stream())
    		.collect(Collectors.toList());
    }

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
