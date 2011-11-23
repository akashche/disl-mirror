package ch.usi.dag.disl.test.guard;

import ch.usi.dag.disl.annotation.GuardMethod;

public abstract class GuardNo {

	@GuardMethod
	public static boolean isApplicable() {

		return false;
	}
}
