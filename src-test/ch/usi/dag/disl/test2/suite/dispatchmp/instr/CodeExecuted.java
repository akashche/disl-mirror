package ch.usi.dag.disl.test2.suite.dispatchmp.instr;

import java.util.concurrent.atomic.AtomicLong;

import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;
import ch.usi.dag.dislreserver.shadow.ShadowObject;

// NOTE that this class is not static anymore
public class CodeExecuted extends RemoteAnalysis {

	long startTime = 0;
	
	AtomicLong totalIntEvents = new AtomicLong();
	AtomicLong totalObjEvents = new AtomicLong();
	AtomicLong totalFreeEvents = new AtomicLong();
	
	public void intEvent(int number) {
		
		if(startTime == 0) {
			startTime = System.nanoTime();
		}
		
		if(totalIntEvents.incrementAndGet() % 1000000 == 0) {
			System.out.println("So far received "
					+ totalIntEvents + " events...");
		}
	}
	
	public void objectEvent(ShadowObject o) {

		totalObjEvents.incrementAndGet();
	}
	
	public void objectFree(ShadowObject netRef) {
		totalFreeEvents.incrementAndGet();
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
