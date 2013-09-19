package ch.usi.dag.disl.test2.suite.guard.instr;

import ch.usi.dag.disl.annotation.GuardMethod;

public abstract class GuardYes {

	@GuardMethod
	public static boolean isApplicable() {

		return true;
	}
}
