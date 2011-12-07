package ch.usi.dag.disl.test.scope;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class DiSLClass {
	
	@AfterReturning(marker = BodyMarker.class)
	public static void onMethodExit(MethodStaticContext msc) {
	    System.out.println("Exiting " + msc.thisMethodFullName());
	}

	@Before(marker = BodyMarker.class, scope = "ch.usi.dag.disl.test.scope.TargetClass.complete(java.lang.String,boolean,boolean)")
	public static void beforeComplete() {
	    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
}
