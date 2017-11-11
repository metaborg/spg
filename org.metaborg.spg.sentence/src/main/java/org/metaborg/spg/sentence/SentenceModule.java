package org.metaborg.spg.sentence;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.metaborg.spg.sentence.signature.SignatureReaderFactory;

public class SentenceModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(SignatureReaderFactory.class));
    }
}
