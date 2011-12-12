package ch.usi.dag.disl.example.sharing.runtime;

import java.lang.ref.WeakReference;

public class FieldState {

	private static final String OFFSET = "\t\t";
	private static final String SEPARATOR = "\t";

	private enum MyState {
		NOT_ACCESSED,
		READ_BY_ALLOCATING_THREAD,
		READ_BY_NON_ALLOCATING_THREAD,
		WRITE_BY_ALLOCATING_THREAD,
		WRITE_BY_NON_ALLOCATING_THREAD
	};

	private final String fieldName;
	private MyState currentState;
	private WeakReference<Thread> allocatingThread;

	public FieldState(String fieldName){
		this.fieldName = fieldName;
		currentState = MyState.NOT_ACCESSED;
	}

	public String getFieldName(){
		return fieldName;
	}

	public synchronized void onRead(Thread accessingThread) {
		switch(currentState) {
		case NOT_ACCESSED:
			currentState = MyState.READ_BY_ALLOCATING_THREAD;
			allocatingThread = new WeakReference<Thread>(accessingThread);
			break;
		case READ_BY_ALLOCATING_THREAD:
		case WRITE_BY_ALLOCATING_THREAD:
			if(allocatingThread.get() != accessingThread) {
				currentState = MyState.READ_BY_NON_ALLOCATING_THREAD;
			}
			break;
		}
	}

	public synchronized void onWrite(Thread accessingThread) {
		switch(currentState) {
		case NOT_ACCESSED: 
			currentState = MyState.WRITE_BY_ALLOCATING_THREAD;
			allocatingThread = new WeakReference<Thread>(accessingThread);
			break;
		case READ_BY_ALLOCATING_THREAD:
		case WRITE_BY_ALLOCATING_THREAD:
			if(allocatingThread.get() != accessingThread) {
				currentState = MyState.WRITE_BY_NON_ALLOCATING_THREAD;
			}
			break;
		}
	}

	//this method is synchronized to guarantee visibility
	public synchronized String toString() {
		return OFFSET + fieldName + SEPARATOR + currentState.name();
	}

}
