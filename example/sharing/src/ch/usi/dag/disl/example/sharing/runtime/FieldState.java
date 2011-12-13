package ch.usi.dag.disl.example.sharing.runtime;

import java.lang.ref.WeakReference;

public class FieldState {

	private static final String OFFSET = "\t\t";
	private static final String SEPARATOR = "\t";
	
	private static final int READ_BY_ALLOCATING_THREAD = 0;
	private static final int WRITE_BY_ALLOCATING_THREAD = 1;
	private static final int READ_BY_NON_ALLOCATING_THREAD = 2;
	private static final int WRITE_BY_NON_ALLOCATING_THREAD = 3;
	
	
	
	// first byte for the read access by allocating thread
	// second byte for the write access by allocating thread
	// third byte for the read access by non-allocating thread
	// fourth byte for the write access by non-allocating thread
	
	private boolean[] accesses = new boolean[4];
	
	private final String fieldName;
	private WeakReference<Thread> allocatingThread;

	public FieldState(String fieldName, Thread thread){
		this.fieldName = fieldName;
		allocatingThread = new WeakReference<Thread>(thread);
	}

	public String getFieldName(){
		return fieldName;
	}

	public synchronized void onRead(Thread accessingThread) {

		if(allocatingThread.get() != accessingThread) {
			accesses[READ_BY_NON_ALLOCATING_THREAD] = true;
		}
		else {
			accesses[READ_BY_ALLOCATING_THREAD] = true;
		}
	}

	public synchronized void onWrite(Thread accessingThread) {

		if(allocatingThread.get() != accessingThread) {
			accesses[WRITE_BY_NON_ALLOCATING_THREAD] = true;
		}
		else {
			accesses[WRITE_BY_ALLOCATING_THREAD] = true;
		}
	}

	//this method is synchronized to guarantee visibility
	public synchronized String toString() {
		
		return OFFSET + fieldName + SEPARATOR 
			+	"read-by-allocating-thread: " + accesses[READ_BY_ALLOCATING_THREAD] + SEPARATOR 
			+	"write-by-allocating-thread: " + accesses[WRITE_BY_ALLOCATING_THREAD] + SEPARATOR 
			+	"read-by-non-allocating-thread: " + accesses[READ_BY_NON_ALLOCATING_THREAD] + SEPARATOR 
			+	"write-by-non-allocating-thread: " + accesses[WRITE_BY_NON_ALLOCATING_THREAD] + SEPARATOR 
						;
	}

}
