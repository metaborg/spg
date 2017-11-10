package org.metaborg.spg.sentence.eclipse;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.metaborg.spg.sentence.eclipse.job.SentenceJobFactory;

public class SentenceModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(SentenceJobFactory.class));
    }
}
