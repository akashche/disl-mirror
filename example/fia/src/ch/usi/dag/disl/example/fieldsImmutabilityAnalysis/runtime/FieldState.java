package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

public class FieldState {

	private final String fieldId;

	private State currentState = State.VIRGIN;

	private long numReads, numWrites;

	private boolean defaultInit = false;

	private enum State { VIRGIN, IMMUTABLE, MUTABLE };

	public FieldState(String fieldId) {
		this.fieldId = fieldId;
	}

	public String getFieldId() { return fieldId; }

	public State getState() { return currentState; }

	public long getNumReads() { return numReads; }

	public long getNumWrites() { return numWrites; }

	public boolean isDefaultInit() { return defaultInit; }

	public synchronized void onRead() {
		numReads++;

		switch (currentState) {
		case VIRGIN:
			defaultInit = true;
			currentState = State.IMMUTABLE;
			break;
		}
	}

	public synchronized void onWrite(boolean isInDynamicExtendOfConstructor) {
		numWrites++;

		switch(currentState) {
		case VIRGIN:
		case IMMUTABLE:
			currentState = isInDynamicExtendOfConstructor ? State.IMMUTABLE : State.MUTABLE;
			break;
		}
	}
}
