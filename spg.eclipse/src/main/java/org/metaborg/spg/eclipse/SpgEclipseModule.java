package org.metaborg.spg.eclipse;

import org.metaborg.spg.eclipse.jobs.IJobFactory;
import org.metaborg.spg.eclipse.jobs.JobFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class SpgEclipseModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(IJobFactory.class).to(JobFactory.class).in(Singleton.class);
	}
}