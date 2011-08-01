package ch.usi.dag.disl.test.dynamicinfo;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamicinfo.DynamicContext;
import ch.usi.dag.disl.snippet.marker.BodyMarker;
import ch.usi.dag.disl.snippet.marker.BytecodeMarker;

public class DiSLClass {

	@Before(marker = BytecodeMarker.class, param = "isub", scope = "TargetClass.test1")
	public static void precondition(DynamicContext di) {
		int i = di.getStackValue(1, int.class);
		int j = di.getStackValue(0, int.class);
		System.out.println(i + " - " + j + " = " + (i - j));
	}
	
	@AfterReturning(marker = BodyMarker.class, param = "ireturn", scope = "TargetClass.test1")
	public static void postcondition(DynamicContext di) {
		int ret = di.getLocalVariableValue(1, int.class);
		System.out.println("before return, local a is " + ret);
	}

	@AfterReturning(marker = BodyMarker.class, param = "ireturn", scope = "TargetClass.test2")
	public static void postcondition2(DynamicContext di) {
		int ret = di.getStackValue(0, int.class);
		System.out.println("Return with " + ret);
	}
	
	@AfterReturning(marker = BodyMarker.class, param = "ireturn", scope = "TargetClass.test3")
	public static void postcondition3(DynamicContext di) {
		double d = di.getLocalVariableValue(1, double.class);
		System.out.println("before return, local d is " + d);
		int i = di.getMethodArgumentValue(1, int.class);
		System.out.println("before return, local i is " + i);
	}
}
