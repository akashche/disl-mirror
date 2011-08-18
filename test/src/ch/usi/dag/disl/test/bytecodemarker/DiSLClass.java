package ch.usi.dag.disl.test.bytecodemarker;

import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.marker.BytecodeMarker;
import ch.usi.dag.disl.staticinfo.analysis.BytecodeAnalysis;

public class DiSLClass {
	
	@Before(marker = BytecodeMarker.class, param="aload, if_icmpne", scope = "TargetClass.main")
	public static void precondition(BytecodeAnalysis ba) {
		System.out.println(ba.getBytecodeNumber());
	}
}
