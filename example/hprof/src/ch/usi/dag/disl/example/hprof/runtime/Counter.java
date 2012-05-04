package ch.usi.dag.disl.example.hprof.runtime;

public class Counter {

    private final String objectType;
    private int totalNumberOfObjects;
    private int currentNumberOfObjects;
    private long totalSize;
    private long currentSize;
    //maybe not the best solution
    private String allocationSite;

    public Counter(String objType) {
        this.objectType = objType;
    }

    public String getObjectType(){
        return objectType;
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

    public void setAllocationSite(String allocationSite) {
        this.allocationSite = allocationSite;
    }

    public synchronized int getTotalNumberOfObjects() {
        return totalNumberOfObjects;
    }

    public synchronized int getCurrentNumberOfObjects() {
        return currentNumberOfObjects;
    }

    public synchronized long getTotalSize(){
        return totalSize;
    }

    public synchronized long getCurrentSize(){
        return currentSize;
    }

    public String getAllocationSite() {
        return allocationSite;
    }

    public String toString() {
        return getCurrentSize() + "\t" + getCurrentNumberOfObjects() + "\t" + getTotalSize() + "\t" + getTotalNumberOfObjects() + "\t" + getObjectType() + "\t" + getAllocationSite(); 
    }
}
