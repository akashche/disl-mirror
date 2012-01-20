package ch.usi.dag.dislagent;

import java.lang.instrument.Instrumentation;

import ch.usi.dag.disl.dynamicbypass.Bootstrap;

public class DiSLAgent {

	public static void premain(String agentArguments,
			Instrumentation instrumentation) {

		if (!Boolean.getBoolean("dislserver.noBootstrap")) {
			Bootstrap.completed(instrumentation);
		}
	}
}
