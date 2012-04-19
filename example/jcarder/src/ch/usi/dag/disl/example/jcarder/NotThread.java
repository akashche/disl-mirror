package ch.usi.dag.disl.example.jcarder;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class NotThread {

	@GuardMethod
	public static boolean isApplicable(MethodStaticContext msc) {
		return !msc.thisClassName().equals("java/lang/Thread");
	}
}
