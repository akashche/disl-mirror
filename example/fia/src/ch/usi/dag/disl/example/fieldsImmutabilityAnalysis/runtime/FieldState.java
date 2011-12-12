package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

public class FieldState {

	private final String fieldId; 
	private long numWrites;
	private long numReads;
	private enum MyState {VIRGIN, IMMUTABLE, MUTABLE };
	private boolean defaultInit = false;
	private MyState currentState;

	public FieldState(String fieldId) {
		this.fieldId = fieldId;
		this.currentState = MyState.VIRGIN;
	}

	public synchronized void onRead() {
		numReads++;

		if(currentState == MyState.VIRGIN) {
			defaultInit = true;
			currentState = MyState.IMMUTABLE;
		}
	}

	public synchronized void onWrite(boolean isInDynamicExtendOfConstructor) {
		numWrites++;

		switch(currentState) {
		case VIRGIN:
			currentState = isInDynamicExtendOfConstructor ? MyState.IMMUTABLE : MyState.MUTABLE;
			break;
		case IMMUTABLE:
			if(!isInDynamicExtendOfConstructor) {
				currentState = MyState.MUTABLE;
			}
			break;
		}
	}

	public String toString() {
		return fieldId + " " + currentState + " default init:" + defaultInit + " writes: " + numWrites + " reads: " + numReads;
	}
}
