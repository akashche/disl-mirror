package ch.usi.dag.disl.test2.suite.dispatch2.app;

import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;
import ch.usi.dag.dislreserver.shadow.ShadowObject;

// NOTE that this class is not static anymore
public class CodeExecuted extends RemoteAnalysis {

	long startTime = 0;
	
	long totalIntEvents = 0;
	long totalObjEvents = 0;
	long totalFreeEvents = 0;
	
	public void intEvent(int number) {
		
		if(startTime == 0) {
			startTime = System.nanoTime();
		}
		
		if(totalIntEvents != number) {
			System.out.println("ERROR in sequence for number "
					+ totalIntEvents);
		}
		
		++totalIntEvents;
		
		if(totalIntEvents % 1000000 == 0) {
			System.out.println("So far received "
					+ totalIntEvents + " events...");
		}
	}
	
	public void objectEvent(ShadowObject o) {

		++totalObjEvents;
	}
	
	public void objectFree(ShadowObject netRef) {
		++totalFreeEvents;
	}
	
	public void atExit() {
		
		System.out.println("Total transport time is "
				+ ((System.nanoTime() - startTime) / 1000000) + " ms");
		
		System.out.println("Total number of int events: "
				+ totalIntEvents);
		
		System.out.println("Total number of object events: "
				+ totalObjEvents);
		
		System.out.println("Total number of free events: "
				+ totalFreeEvents);
	}
}
