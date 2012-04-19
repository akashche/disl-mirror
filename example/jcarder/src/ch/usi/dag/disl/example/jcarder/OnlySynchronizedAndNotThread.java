package ch.usi.dag.disl.example.jcarder;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.guardcontext.GuardContext;

public class OnlySynchronizedAndNotThread {

	@GuardMethod
	public static boolean isApplicable(GuardContext gc) {
		return gc.invoke(NotThread.class) && gc.invoke(OnlySynchronized.class);  
	}
}
