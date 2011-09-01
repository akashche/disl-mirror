package ch.usi.dag.disl.test.scope;

import ch.usi.dag.disl.dislclass.annotation.AfterReturning;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;

public class DiSLClass {
	
	@AfterReturning(marker = BodyMarker.class, order = 0, scope = "*.*")
	public static void onMethodExit() {
	    System.out.println("????????????????????????????");
	}

	@Before(marker = BodyMarker.class, order = 1, scope = "ch.usi.dag.disl.test.scope.TargetClass.complete(java.lang.String,boolean,boolean)")
	public static void beforeComplete() {
	    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
}
