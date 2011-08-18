package ch.usi.dag.disl.test.staticinfo;

import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;
import ch.usi.dag.disl.staticinfo.analysis.StaticContext;

public class DiSLClass {
	
	@Before(marker = BodyMarker.class, scope = "TargetClass.this_is_a_method_name", order = 0)
	public static void precondition(StaticContext ci) {
		
		String mid = ci.thisMethodName();
		System.out.println(mid);
		
		// caching test
		String mid2 = ci.thisMethodName();
		System.out.println(mid2);
	}
	
	@Before(marker = BodyMarker.class, scope = "TargetClass.this_is_a_method_name", order = 1)
	public static void secondPrecondition(StaticContext ci) {
		
		// caching test
		String mid3 = ci.thisMethodName();
		System.out.println(mid3);
	}
}
