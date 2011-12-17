package ch.usi.dag.disl.test.pe2;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class DiSLClass {

	@Before(marker = BodyMarker.class, scope = "TargetClass.*", order = 0)
	public static void precondition(MethodStaticContext msc) {

		if (!msc.thisMethodName().equals("<clinit>")
				&& !(msc.thisMethodName().equals("<init>") && (msc
						.thisClassName().equals("java/lang/Object") || msc
						.thisClassName().equals("java/lang/Thread")))) {
			System.out.println("Go ahead");
		} else {
			System.out.println("Not a good idea to weave some code here");
		}

		if (msc.thisMethodName().endsWith("init>")) {
			System.out.println("init or clniit");
		}

		if (String.valueOf(true).equals("true")) {
			System.out.println("this shold be printed");
		}
	}
}
