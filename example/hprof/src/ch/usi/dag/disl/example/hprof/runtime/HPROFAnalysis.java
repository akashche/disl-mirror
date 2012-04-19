package ch.usi.dag.disl.example.hprof.runtime;



import java.lang.instrument.Instrumentation;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ch.usi.dag.disl.dynamicbypass.DynamicBypass;
import ch.usi.dag.dislagent.DiSLAgent;

public class HPROFAnalysis {

	private class ReaperThread extends Thread {
		public ReaperThread() {
			setDaemon(true);
		}

		public void run() {
			DynamicBypass.activate();
			//TODO: maybe this should be an int --> call gc multiple times in a row if not refs are available
			boolean blocking = false;

			while(true) {
				try {
					Object obj;
//					if(!blocking) {
//						obj = refqueue.poll();
//					} else {
						obj = refqueue.remove();
//					}

//					if(obj != null) {
//						blocking = false;
						MyWeakReference mwr = weakReferenceMap.remove(obj);
						if(mwr != null) {
							String fullAllocSiteID = mwr.getFullAllocSiteID();
							counterMap.get(fullAllocSiteID).decrementCounters(mwr.getSize());
						} else {
							//TODO: write something more meaningful
							System.err.println("WEIRD! THE WEAKREFERENCE WAS NOT IN THE MAP!");
						}
//					} else {
//						blocking = true;
//						callGC(0);
//					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	//HashMap that contains object IDs as a key and an object of type MyWeakReference as the value
	private final ConcurrentHashMap<Reference<? extends Object>, MyWeakReference> weakReferenceMap;
	
	//HashMap that contains object IDs as a key and a wrapper object of type Counter as the value
	protected static final ConcurrentHashMap<String, Counter> counterMap;
	
	private static final ReferenceQueue<Object> refqueue;
	private final Thread reaper;


	private static final HPROFAnalysis instanceOfHPROF;

	private static final Instrumentation instr;

	static {
		instanceOfHPROF = new HPROFAnalysis();
		refqueue = new ReferenceQueue<Object>();
		counterMap = new ConcurrentHashMap<String, Counter>();
		instr = DiSLAgent.getInstrumentation();		
	}



	

	private HPROFAnalysis() {
	
		weakReferenceMap = new ConcurrentHashMap<Reference<? extends Object>, MyWeakReference>();

		reaper = new ReaperThread();
		reaper.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				DynamicBypass.activate();

//				
				System.err.println("In shutdown hook!");
//				
				try {
//					
						Iterator<Entry<String, Counter>> iterator = counterMap.entrySet().iterator();
						int idx = 0;
						while (iterator.hasNext()) {  
							idx++;
							Entry<String, Counter> entry = iterator.next();
							String key = entry.getKey(); 
							Counter myCounter = entry.getValue();

							//TODO: myCounter.toString();
							int totalNumber = myCounter.getTotalNumberOfObjects(); 
							long totalSize = myCounter.getTotalSize();
							long currentSize = myCounter.getCurrentSize();
							int currentNumber = myCounter.getCurrentNumberOfObjects();
							String typeOfObject = myCounter.getObjectType();
							System.err.println(idx + " TOTAL SIZE " + totalSize + " TOTAL # " + totalNumber + " CURRENT SIZE " + currentSize + " CURRENT # "+ currentNumber + " TYPE OF THE OBJECT " + typeOfObject + " ALLOCATION SITE: " + key  );
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
				
			}
		});
	}

	public static HPROFAnalysis instanceOf() {
		return instanceOfHPROF;
	}

	public void onObjectInitialization(Object allocatedObj, String allocSiteID, int clID) {
		try {
			long size = instr.getObjectSize(allocatedObj);
			String fullAllocSiteID = allocSiteID + clID;
			MyWeakReference myReference;
			Counter myCounter;
			//TODO: do this at instrumentation time? (may require a @SyntheticLocal stack...)
			String objType = allocatedObj.getClass().getName();

			myReference = new MyWeakReference(allocatedObj, fullAllocSiteID, size, refqueue);

			weakReferenceMap.put(myReference, myReference);

			if ((myCounter = counterMap.get(fullAllocSiteID)) == null) {
				myCounter = new Counter(objType);
				Counter tmpCounter;
				if((tmpCounter = counterMap.putIfAbsent(fullAllocSiteID, myCounter)) != null) {
					myCounter = tmpCounter;
				}
			}
				
			myCounter.incrementCounter(size);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static void callGC(long sleepTime) {
		System.runFinalization();
		System.gc();
		if(sleepTime > 0) {
			try { Thread.sleep(sleepTime); } catch(InterruptedException e) { e.printStackTrace(); }
		}
	}
}
