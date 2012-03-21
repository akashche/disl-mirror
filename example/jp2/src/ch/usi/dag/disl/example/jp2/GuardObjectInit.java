package ch.usi.dag.disl.example.jp2;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public abstract class GuardObjectInit {
	@GuardMethod
	public static boolean isApplicable(MethodStaticContext msc) {

		if (msc.thisClassName().equals("java/lang/Object")
				&& msc.thisMethodName().equals("<init>"))
			return true;
		else {
			return false;
		}
	}
}
