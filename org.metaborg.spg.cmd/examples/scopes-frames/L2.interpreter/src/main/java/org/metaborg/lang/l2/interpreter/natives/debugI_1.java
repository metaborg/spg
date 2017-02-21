package org.metaborg.lang.l2.interpreter.natives;

import org.metaborg.lang.l2.interpreter.natives.debugI_1NodeGen;
import org.metaborg.meta.lang.dynsem.interpreter.nodes.building.TermBuild;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

@NodeChild(value = "child", type = TermBuild.class)
public abstract class debugI_1 extends TermBuild {

	public debugI_1(SourceSection source) {
		super(source);
	}

	@Specialization
	public int doDebug(int i) {
		System.err.println(i);
		
		return i;
	}

	public static TermBuild create(SourceSection source, TermBuild child) {
		return debugI_1NodeGen.create(source, child);
	}

}
