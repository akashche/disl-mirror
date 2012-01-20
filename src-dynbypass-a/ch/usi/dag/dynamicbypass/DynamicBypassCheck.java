package ch.usi.dag.dynamicbypass;


public class DynamicBypassCheck {

	// this version is executed after bootstrap phase
	// and is it the replacement for version in src-dynbypass directory
	public static boolean executeUninstrumente() {
		return DynamicBypass.isActivated();
	}
}
