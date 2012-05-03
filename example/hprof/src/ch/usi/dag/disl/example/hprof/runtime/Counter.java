package ch.usi.dag.disl.example.hprof.runtime;

public class Counter {


	private int totalNumberOfObjects;
	private int currentNumberOfObjects;
	private final String objectType;
	private long totalSize;
	private long currentSize;
	//maybe not the best solution
	private String allocationSite;

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
	
	public synchronized void setAllocationSite(String allocationSite) {
		this.allocationSite = allocationSite;
	}
	
	public String getAllocationSite() {
		return allocationSite;
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
		return getCurrentSize() + "\t" + getCurrentNumberOfObjects() + "\t" + getTotalSize() + "\t" + getTotalNumberOfObjects() + "\t" + getObjectType() + "\t" + getAllocationSite(); 
	}
}
