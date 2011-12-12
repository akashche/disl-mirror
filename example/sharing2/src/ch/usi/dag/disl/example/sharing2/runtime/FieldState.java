package ch.usi.dag.disl.example.sharing2.runtime;

import java.lang.ref.WeakReference;

public class FieldState  {
	private static final String OFFSET = "\t\t";
	private static final String SEPARATOR = "\t";
	
	private enum MyState {VIRGIN, SHARED, EXCLUSIVE};

	private final String fieldName;
	private MyState currentState;
	private WeakReference<Thread> lastAccessingThread;

	private long numberOfAccesses;
	private long concurrencyCounter;
	
	public FieldState(String fieldName){
		this.fieldName = fieldName;
		currentState = MyState.VIRGIN;
	}

	public String getFieldName(){
		return fieldName;
	}

	public synchronized void onFieldAccess(Thread accessingThread) {
		numberOfAccesses++;

		switch(currentState) {
			case VIRGIN:
				currentState = MyState.EXCLUSIVE;
				lastAccessingThread = new WeakReference<Thread>(accessingThread);
				break;
			case EXCLUSIVE:
				if(lastAccessingThread.get() != accessingThread) {
					currentState = MyState.SHARED;
					concurrencyCounter++;
					lastAccessingThread = new WeakReference<Thread>(accessingThread);
				}
				break;
			case SHARED:
				if(lastAccessingThread.get() != accessingThread) {
					concurrencyCounter++;
					lastAccessingThread = new WeakReference<Thread>(accessingThread);
				}
				break;
		}
	}

	//this method is synchronized to guarantee visibility
	public synchronized String toString() {
		return OFFSET + fieldName + SEPARATOR + currentState.name() + SEPARATOR + "number of accesses: " + numberOfAccesses + SEPARATOR + "concurrency counter: " + concurrencyCounter;
	}
}
