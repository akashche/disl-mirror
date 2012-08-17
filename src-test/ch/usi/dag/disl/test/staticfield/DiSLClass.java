package ch.usi.dag.disl.test.staticfield;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticStaticField;
import ch.usi.dag.disl.annotation.SyntheticStaticField.Scope;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class DiSLClass {

	@SyntheticStaticField(scope = Scope.PERCLASS)
	public static String s;

	@SyntheticStaticField(scope = Scope.PERMETHOD)
	public static String mid;

	@Before(marker = BodyMarker.class, scope = "TargetClass.*", order = 2)
	public static void precondition(MethodStaticContext msc) {

		mid = msc.thisMethodFullName();
		System.out.println("Entering " + mid + " while s is " + s);
	}

	@AfterReturning(marker = BodyMarker.class, scope = "TargetClass.*", order = 2)
	public static void postcondition() {
		System.out.println("Exiting " + mid + " while s is " + s);
	}

	@Before(marker = BodyMarker.class, scope = "TargetClass.print", order = 1)
	public static void precondition2() {
		s = "Set in TargetClass.print";
	}

}
