package org.metaborg.spg.eclipse;

import org.metaborg.spg.eclipse.jobs.IJobFactory;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class SpgEclipseModule extends AbstractModule {
	@Override
	protected void configure() {
		install(new FactoryModuleBuilder().build(IJobFactory.class));
	}
}