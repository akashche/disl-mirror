package ch.usi.dag.disl.test.newobjmarker;

import ch.usi.dag.disl.dislclass.annotation.AfterReturning;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.marker.NewObjMarker;
import ch.usi.dag.disl.dynamicinfo.DynamicContext;

public class DiSLClass {
	
	@Before(marker = NewObjMarker.class, scope = "TargetClass.main")
	public static void beforeAlloc() {

		System.out.print("Here we go: ");
	}
	
	@AfterReturning(marker = NewObjMarker.class, scope = "TargetClass.main")
	public static void afterAlloc(DynamicContext dc) {

		TargetClass tc = dc.getStackValue(0, TargetClass.class);
		System.out.println(tc.printMe);
	}
}
