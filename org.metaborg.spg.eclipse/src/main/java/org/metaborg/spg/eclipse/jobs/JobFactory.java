package org.metaborg.spg.eclipse.jobs;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.config.ILanguageComponentConfigService;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.spg.core.Generator;
import org.metaborg.spg.core.SyntaxGenerator;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;

import com.google.inject.Inject;

public class JobFactory implements IJobFactory {
    protected IResourceService resourceService;
    protected IProjectService projectService;
    protected ILanguageService languageService;
    protected ILanguageComponentConfigService configService;
    protected ISourceTextService sourceTextService;
    protected ISpoofaxUnitService unitService;
    protected ISpoofaxSyntaxService syntaxService;
    
    protected SyntaxGenerator syntaxGenerator;
    protected Generator generator;
    
    @Inject
	public JobFactory(IResourceService resourceService, IProjectService projectService, ILanguageService languageService, ILanguageComponentConfigService configService, ISourceTextService sourceTextService, ISpoofaxUnitService unitService, ISpoofaxSyntaxService syntaxService, SyntaxGenerator syntaxGenerator, Generator generator) {
		this.resourceService = resourceService;
		this.projectService = projectService;
		this.languageService = languageService;
		this.configService = configService;
		this.sourceTextService = sourceTextService;
		this.unitService = unitService;
		this.syntaxService = syntaxService;
		
		this.syntaxGenerator = syntaxGenerator;
		this.generator = generator;
	}
    
    @Override
    public GenerateJob createGenerateJob(FileObject project, int termLimit, int termSize, int fuel) {
    	return new GenerateJob(
			// Dependencies
			resourceService,
			projectService,
			languageService,
			configService,
			generator,
			
			// Job configuration
			project,
			termLimit,
			termSize,
			fuel
		);
    }
    
    @Override
	public AmbiguityJob createAmbiguityJob(FileObject project, int termLimit, int fuel) {
    	return new AmbiguityJob(
			// Dependencies
			resourceService,
			projectService,
			languageService,
			configService,
			sourceTextService,
			unitService,
			syntaxService,
			syntaxGenerator,
			
			// Job configuration
			project,
			termLimit,
			fuel
		);
    }
    
	@Override
	public SoundnessJob createSoundnessJob(FileObject project, int termLimit, int termSize, int fuel, boolean store, String interpreter, int timeout) {
		return new SoundnessJob(
			// Dependencies
			resourceService,
			projectService,
			languageService,
			configService,
			generator,
			
			// Job configuration
			project,
			termLimit,
			termSize,
			fuel,
			store,
			interpreter,
			timeout
		);
	}
}
