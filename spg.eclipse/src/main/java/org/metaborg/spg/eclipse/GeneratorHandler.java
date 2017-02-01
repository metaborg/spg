package org.metaborg.spg.eclipse;

import rx.Observable;
import rx.functions.Action1;

import org.metaborg.spg.core.Config;
import org.metaborg.spg.core.GeneratorEntryPoint;

public class GeneratorHandler {
	public GeneratorHandler() {
		// Generate infinite terms (-1) with fuel (500) and limit size (100)
		Config config = new Config(-1, 500, 100, true, true);

		Observable<? extends String> programs = new GeneratorEntryPoint()
			.generate("", "", "", "", config)
			.asJavaObservable();
		
		programs.subscribe(new Action1<String>() {
			@Override
			public void call(String program) {
				System.out.println(program);
			}
		});
	}
}
