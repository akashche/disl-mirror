package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class OnlyInit {
	@GuardMethod
	public static boolean isApplicable(MethodStaticContext msc) {
		return msc.thisMethodName().equals("<init>");
	}
}
