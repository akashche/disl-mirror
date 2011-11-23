package ch.usi.dag.disl.example.map;

import ch.usi.dag.disl.annotation.GuardMethod;

//check if the method has at least one monitor enter on its body.
public class HasMonitorGuard {
	
	@GuardMethod
	public static boolean isApplicable(MonitorSC msc) {
		return msc.hasMonitor();
	}
}
