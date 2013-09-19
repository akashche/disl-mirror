package ch.usi.dag.disl.test2.suite.guard.instr;

import ch.usi.dag.disl.annotation.GuardMethod;

public abstract class GuardNo {

	@GuardMethod
	public static boolean isApplicable() {

		return false;
	}
}
