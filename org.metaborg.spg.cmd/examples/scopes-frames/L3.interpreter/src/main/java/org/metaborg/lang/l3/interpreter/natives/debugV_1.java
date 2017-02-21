package org.metaborg.lang.l3.interpreter.natives;

import org.metaborg.lang.l3.interpreter.natives.debugV_1NodeGen;
import org.metaborg.meta.lang.dynsem.interpreter.nodes.building.TermBuild;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

@NodeChild(value = "child", type = TermBuild.class)
public abstract class debugV_1 extends TermBuild {

	public debugV_1(SourceSection source) {
		super(source);
	}

	@Specialization
	public Object doDebug(Object o) {
		System.err.println(o.toString());
		
		return o;
	}

	public static TermBuild create(SourceSection source, TermBuild child) {
		return debugV_1NodeGen.create(source, child);
	}

}
