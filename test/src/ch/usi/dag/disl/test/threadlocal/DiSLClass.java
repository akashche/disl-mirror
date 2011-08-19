package ch.usi.dag.disl.test.threadlocal;

import ch.usi.dag.disl.dislclass.annotation.After;
import ch.usi.dag.disl.dislclass.annotation.ThreadLocal;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;

public class DiSLClass {
    
	@ThreadLocal
	static String tlv = "ahoj";
	
	@After(marker = BodyMarker.class, scope = "TargetClass.foo", order=0)
	public static void postcondition() {
		System.out.println(tlv);
	}
}
