package org.metaborg.spg.sentence.sdf.eclipse;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.metaborg.spg.sentence.sdf.eclipse.job.JobFactory;
import org.metaborg.spg.sentence.guice.SentenceModule;

public class SentenceEclipseModule extends SentenceModule {
    @Override
    protected void configure() {
        super.configure();

        install(new FactoryModuleBuilder().build(JobFactory.class));
    }
}
