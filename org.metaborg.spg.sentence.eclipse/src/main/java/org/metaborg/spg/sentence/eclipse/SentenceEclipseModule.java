package org.metaborg.spg.sentence.eclipse;

import com.google.inject.assistedinject.FactoryModuleBuilder;

import org.metaborg.spg.sentence.SentenceModule;
import org.metaborg.spg.sentence.eclipse.job.SentenceJobFactory;

public class SentenceEclipseModule extends SentenceModule {
    @Override
    protected void configure() {
        super.configure();
    		
        install(new FactoryModuleBuilder().build(SentenceJobFactory.class));
    }
}
