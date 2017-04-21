package org.metaborg.spg.eclipse.jobs;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.SyntaxGenerator;
import org.metaborg.spg.core.SyntaxGeneratorFactory;
import org.metaborg.spg.core.SyntaxShrinker;
import org.metaborg.spg.core.spoofax.*;
import org.metaborg.spg.eclipse.Activator;
import org.metaborg.spg.eclipse.rx.MapWithIndex;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;

public class AmbiguityJob extends Job {
	public static Charset UTF_8 = StandardCharsets.UTF_8;
	
    protected MessageConsole console = ConsoleUtils.get("Spoofax console");
    protected MessageConsoleStream stream = console.newMessageStream();
    
    protected IResourceService resourceService;
    protected ISourceTextService sourceTextService;
    protected ISpoofaxUnitService unitService;
    protected ISpoofaxSyntaxService syntaxService;
    protected SyntaxGeneratorFactory generatorFactory;
    protected ParseService parseService;
    
	protected IProject project;
	protected ILanguageImpl language;
	protected Config config;
    
	@Inject
	public AmbiguityJob(
		IResourceService resourceService,
		ISourceTextService sourceTextService,
		ISpoofaxUnitService unitService,
		ISpoofaxSyntaxService syntaxService,
		SyntaxGeneratorFactory generatorFactory,
		ParseService parseService,
		
		@Assisted IProject project,
		@Assisted ILanguageImpl language,
		@Assisted Config config
	) {
		super("Generate");
		
		this.resourceService = resourceService;
		this.sourceTextService = sourceTextService;
		this.unitService = unitService;
		this.syntaxService = syntaxService;
		
		this.generatorFactory = generatorFactory;
		this.parseService = parseService;
		
		this.project = project;
		this.language = language;
		this.config = config;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final SubMonitor subMonitor = SubMonitor.convert(monitor, config.limit());
		
		final Language language = generatorFactory.loadLanguage(this.language, project);
		
		final SyntaxGenerator generator = generatorFactory.create(this.language, project, config);
		
		final SyntaxShrinker shrinker = new SyntaxShrinker(generator, parseService, language, new scala.util.Random(0));

		@SuppressWarnings("unchecked")
		Observable<String> programs = (Observable<String>) generator
			.generate()
			.asJavaObservable();
		
		programs
			.doOnNext(program -> progress(subMonitor, program))
			.map(program -> store(program))
			.map(file -> parse(this.language, file))
			.filter(parseUnit -> parseUnit.valid())
			.takeFirst(parseUnit -> ambiguous(parseUnit))
			.map(parseUnit -> shrink(shrinker, parseUnit))
			.subscribe(parseUnit -> {
				IStrategoTerm ast = parseUnit.ast();
				
				stream.println("=== Shrunk program ===");
				stream.println(parseUnit.input().text());
				
				stream.println("=== Ambiguities ===");
				stream.println(Joiner.on("\n").join(ambiguities(ast).collect(Collectors.toList())));
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
    };
    
    /**
     * Show there is some progress by printing the program and incrementing
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
     * Check if the program is ambiguous.
     * 
     * @param program
     * @return
     */
    protected boolean ambiguous(String program) {
    	ISpoofaxParseUnit parseUnit = parse(language, store(program));
    	
    	return ambiguous(parseUnit);
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
        return ambiguities(term).findAny().isPresent();
    }

    /**
      * Compute a lazy stream of all 'amb' constructors.
      * 
      * @param term
      * @return
      */
    protected Stream<IStrategoTerm> ambiguities(IStrategoTerm term) {
	    if (term instanceof IStrategoAppl) {
		    IStrategoAppl appl = (IStrategoAppl) term;
		    
		    if (appl.getConstructor().getName() == "amb") {
			    return Stream.of(term);
		    }
	    }

	    return Arrays
    		.stream(term.getAllSubterms())
    		.flatMap(child -> ambiguities(child));
    }
    
    /**
     * Recursively shrink the given program until it is no longer ambiguous or
     * cannot be shrunk any further.
     * 
     * @param shrinker
     * @param program
     * @return
     */
    protected ISpoofaxParseUnit shrink(SyntaxShrinker shrinker, ISpoofaxParseUnit parseUnit) {
		stream.println("=== Shrink ===");
		stream.println(parseUnit.input().text());
		
		@SuppressWarnings("unchecked")
		Observable<String> shrunkPrograms = (Observable<String>) shrinker
			.shrink(parseUnit.input().text())
			.asJavaObservable();
		
		return shrunkPrograms
			.map(shrunkProgram -> parse(language, store(shrunkProgram)))
			.filter(shrunkParseUnit -> ambiguous(shrunkParseUnit))
			.map(shrunkParseUnit -> shrink(shrinker, shrunkParseUnit))
			.firstOrDefault(parseUnit)
			.toBlocking()
			.single();
    }
}
