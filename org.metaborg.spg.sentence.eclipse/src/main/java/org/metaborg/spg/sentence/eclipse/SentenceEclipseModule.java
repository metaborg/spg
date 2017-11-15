package org.metaborg.spg.sentence.eclipse;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.metaborg.spg.sentence.eclipse.job.SentenceJobFactory;
import org.metaborg.spg.sentence.guice.SentenceModule;

public class SentenceEclipseModule extends SentenceModule {
    @Override
    protected void configure() {
        super.configure();

        install(new FactoryModuleBuilder().build(SentenceJobFactory.class));
    }
}
