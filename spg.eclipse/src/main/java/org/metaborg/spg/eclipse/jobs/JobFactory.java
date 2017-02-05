package org.metaborg.spg.eclipse.jobs;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.config.ILanguageComponentConfigService;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spg.core.Generator;

import com.google.inject.Inject;

public class JobFactory implements IJobFactory {
    protected IResourceService resourceService;
    protected IProjectService projectService;
    protected ILanguageService languageService;
    protected ILanguageComponentConfigService configService;
    protected Generator generator;
    
    @Inject
	public JobFactory(IResourceService resourceService, IProjectService projectService, ILanguageService languageService, ILanguageComponentConfigService configService, Generator generator) {
		this.resourceService = resourceService;
		this.projectService = projectService;
		this.languageService = languageService;
		this.configService = configService;
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
	public SoundnessJob createSoundnessJob(FileObject project, int termLimit, int termSize, int fuel, int timeout, boolean store) {
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
			timeout,
			store
		);
	}
}
