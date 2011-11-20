package ch.usi.dag.disl.test.pe;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.staticcontext.MethodSC;

public class DiSLClass {
	
	@Before(marker = BodyMarker.class, scope = "TargetClass.*", order = 0)
	public static void precondition(MethodSC msc) {
		
		if (msc.isMethodPrivate()){
			System.out.println("this is a private method.");
		} else {
			System.out.println("this is a public method.");
		}
	}
}
