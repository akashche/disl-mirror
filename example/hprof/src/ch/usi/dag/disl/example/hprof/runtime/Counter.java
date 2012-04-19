package ch.usi.dag.disl.example.hprof.runtime;

public class Counter {


	private int totalNumberOfObjects;
	private int currentNumberOfObjects;
	private final String objectType;
	private long totalSize;
	private long currentSize;


	public Counter(String objType) {
		this.objectType = objType;
	}
	
	public int getTotalNumberOfObjects() {
		return totalNumberOfObjects;
	}
	
	public long getTotalSize(){
		return totalSize;
	}
	
	public long getCurrentSize(){
		return currentSize;
	}

	public int getCurrentNumberOfObjects() {
		return currentNumberOfObjects;
	}
	

	
	public synchronized void incrementCounter(long size) {
		totalNumberOfObjects++;
		currentNumberOfObjects++;
		this.totalSize += size;
		this.currentSize += size;
	}
	
	public synchronized void decrementCounters(long size) {
		currentNumberOfObjects--;
		this.currentSize -= size;
	}
	
	public String getObjectType(){
		return objectType;
	}

	public String toString() {
		return getObjectType() + "\t" + "..."; 
	}
}
