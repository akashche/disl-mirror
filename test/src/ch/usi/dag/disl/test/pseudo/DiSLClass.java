package ch.usi.dag.disl.test.pseudo;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.marker.BodyMarker;
import ch.usi.dag.disl.staticinfo.analysis.ContextInfo;

public class DiSLClass {
	
	@Before(marker = BodyMarker.class, scope = "TargetClass.this_is_a_method_name", order = 0)
	public static void precondition(ContextInfo ci) {
		String mid = ci.getMethodName();
		System.out.println(mid);
	}
}
