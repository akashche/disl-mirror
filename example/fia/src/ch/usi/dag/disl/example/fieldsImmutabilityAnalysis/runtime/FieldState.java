package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.io.PrintStream;

public class FieldState  {
	private static final String OFFSET = "\t\t";
	private static final String SEPARATOR = "\t";

	private enum MyState {VIRGIN, IMMUTABLE, MUTABLE};

	private MyState currentState;
	private long writeAccesses;
	private boolean defaultInit = true;
	private final String fieldName;
	
	public FieldState(String fieldName){
		this.fieldName = fieldName;
		currentState = MyState.VIRGIN;
	}

	public String getFieldName(){
		return fieldName;
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
				defaultInit = false;
				if(isInDynamicExtendOfConstructor) {
					currentState = MyState.IMMUTABLE;
				}
			case IMMUTABLE:
				writeAccesses++;
				if(!isInDynamicExtendOfConstructor) {
					currentState = MyState.MUTABLE;
				}
				else{
					defaultInit = false;
				}
				break;
		}
	}

	public synchronized void dump(PrintStream ps) {
		ps.println(OFFSET + fieldName + SEPARATOR + currentState.name() +  " : " + defaultInit +  " : " + "[writes: " + writeAccesses + "]" );
	}

}
