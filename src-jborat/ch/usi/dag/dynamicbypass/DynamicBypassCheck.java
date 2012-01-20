package ch.usi.dag.dynamicbypass;

public class DynamicBypassCheck {

	// this version is executed before bootstrap phase
	// and will be replaced by ...AfterBootstrap() after bootstrap
	public static boolean executeUninstrumented() {
		return true;
	}
	
	public static boolean executeUninstrumentedAfterBootstrap() {
		return DynamicBypass.isActivated();
	}
}
