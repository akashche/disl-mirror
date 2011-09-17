package ch.usi.dag.disl.test.threadlocal;

import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.annotation.After;
import ch.usi.dag.disl.dislclass.annotation.ThreadLocal;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;

public class DiSLClass {
    
	@ThreadLocal
	static String tlv = "ahoj";
	
	
	@Before(marker = BodyMarker.class, scope = "*.foo*", order=0)
	public static void precondition() {
		System.out.println("pre \t" + Thread.currentThread().toString() + " \t tlv " +  tlv);
		tlv = "hello";
	}
	
	@After(marker = BodyMarker.class, scope = "*.foo*", order=0)
	public static void postcondition() {
		System.out.println("post \t" + Thread.currentThread().toString() + " \t tlv "  + tlv);
	}
}
