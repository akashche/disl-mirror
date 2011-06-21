package ch.usi.dag.disl.test.agent;

import java.lang.instrument.Instrumentation;

public class Agent {

	// INSTRUCTIONS: run ant agent
	// under Eclipse create runner for desired target class
	// add these jvm parameters (example)
	// -javaagent:build/agent-light.jar
	// -Ddisl.classes=bin/ch/usi/dag/disl/test/bodymarker/DiSLClass.class
	
    /**
     * Premain method is a method called when the agent is loaded. As the name
     * says, the method is called before the application main method.
     * 
     * @param agentArguments
     * @param instrumentation
     */
    public static void premain (String agentArguments,
        Instrumentation instrumentation) {

        instrumentation.addTransformer(new Transformer());
    }

}
