package ch.usi.dag.jborat.agent;

import java.lang.instrument.Instrumentation;

import ch.usi.dag.dynamicbypass.Bootstrap;

public class JavaAgent {

	public static void premain(String agentArguments,
			Instrumentation instrumentation) {

		if (!Boolean.getBoolean("jborat.noBootstrap")) {
			Bootstrap.completed();
		}
	}
}
