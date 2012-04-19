package ch.usi.dag.disl.example.jcarder;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class OnlySynchronized {

	@GuardMethod
	public static boolean isApplicable(MethodStaticContext msc) {
		return msc.isMethodSynchronized();
	}
}
