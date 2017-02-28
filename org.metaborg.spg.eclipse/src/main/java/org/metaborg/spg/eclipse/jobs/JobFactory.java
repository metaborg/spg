package org.metaborg.spg.eclipse.jobs;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.spg.core.Generator;
import org.metaborg.spg.core.SyntaxGenerator;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;

import com.google.inject.Inject;

public class JobFactory implements IJobFactory {
    protected IResourceService resourceService;
    protected ISourceTextService sourceTextService;
    protected ISpoofaxUnitService unitService;
    protected ISpoofaxSyntaxService syntaxService;
    
    protected SyntaxGenerator syntaxGenerator;
    protected Generator generator;
    
    @Inject
	public JobFactory(IResourceService resourceService, ISourceTextService sourceTextService, ISpoofaxUnitService unitService, ISpoofaxSyntaxService syntaxService, SyntaxGenerator syntaxGenerator, Generator generator) {
		this.resourceService = resourceService;
		this.sourceTextService = sourceTextService;
		this.unitService = unitService;
		this.syntaxService = syntaxService;
		
		this.syntaxGenerator = syntaxGenerator;
		this.generator = generator;
	}
    
    @Override
    public GenerateJob createGenerateJob(IProject project, ILanguageImpl language, int termLimit, int termSize, int fuel) {
    	return new GenerateJob(
			// Dependencies
			generator,
			
			// Job configuration
			project,
			language,
			termLimit,
			termSize,
			fuel
		);
    }
    
    @Override
	public AmbiguityJob createAmbiguityJob(IProject project, ILanguageImpl language, int termLimit, int fuel) {
    	return new AmbiguityJob(
			// Dependencies
			resourceService,
			sourceTextService,
			unitService,
			syntaxService,
			syntaxGenerator,
			
			// Job configuration
			project,
			language,
			termLimit,
			fuel
		);
    }
    
	@Override
	public SoundnessJob createSoundnessJob(IProject project, ILanguageImpl language, int termLimit, int termSize, int fuel, String interpreter, int timeout) {
		return new SoundnessJob(
			// Dependencies
			generator,
			
			// Job configuration
			project,
			language,
			termLimit,
			termSize,
			fuel,
			interpreter,
			timeout
		);
	}
}
