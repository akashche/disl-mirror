package ch.usi.dag.disl.test.cflow;


import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.annotation.After;
import ch.usi.dag.disl.dislclass.annotation.ThreadLocal;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;
import ch.usi.dag.disl.staticinfo.analysis.StaticContext;

// Simple cflow with DiSL

public class DiSLClass {
	
	@ThreadLocal
    static int counter;

	
	@Before(marker = BodyMarker.class, scope = "TargetClass.foo()", order=0)
	public static void precondition() {
		 counter++;
	}
	

	@After(marker = BodyMarker.class, scope = "TargetClass.foo()", order=0)
	public static void postcondition() {
		counter--;
	}
	
	
	@Before(marker = BodyMarker.class, scope = "TargetClass.*(...)", order=1)
	public static void something(StaticContext sc) {
		 if(counter>0) {
			System.out.println("IN CFLOW OF foo() " + sc.thisMethodFullName());
		}else{
			System.out.println("NOT IN CFLOW OF foo() " + sc.thisMethodFullName());
		}
		
	}
}
