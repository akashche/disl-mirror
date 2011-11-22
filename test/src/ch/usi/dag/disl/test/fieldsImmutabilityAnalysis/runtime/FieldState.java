package ch.usi.dag.disl.test.fieldsImmutabilityAnalysis.runtime;

import java.io.PrintStream;

public class FieldState {

	private enum MyState {VIRGIN, IMMUTABLE, MUTABLE};


	private MyState currentState;
	private long writeAccesses;
	private boolean defaultInit=true;
	
	public FieldState(){
	
		currentState = MyState.VIRGIN;
	}

	
	public synchronized void onRead() {
		switch(currentState) {
			case VIRGIN:
				currentState = MyState.IMMUTABLE;
				break;
		}
	}

	public synchronized void onWrite(boolean isInDynamicExtendOfConstructor) {
		switch(currentState) {
			case VIRGIN: 
				defaultInit=false;
				if(isInDynamicExtendOfConstructor) {
					currentState = MyState.IMMUTABLE;
				}
			case IMMUTABLE:
				writeAccesses++;
				if(!isInDynamicExtendOfConstructor) {
					currentState = MyState.MUTABLE;
				}
				else{
					defaultInit=false;
				}
				break;
		}
	}

	public synchronized void dump(PrintStream ps) {
		ps.println(toString());
	}

	public synchronized String toString() {
		return currentState.name() +  " : " + defaultInit +  " : " + "[writes: " + writeAccesses + "]" ;
	}

	
}