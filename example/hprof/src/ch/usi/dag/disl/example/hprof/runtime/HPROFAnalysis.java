package ch.usi.dag.disl.example.hprof.runtime;



import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import ch.usi.dag.disl.dynamicbypass.DynamicBypass;
import ch.usi.dag.dislagent.DiSLAgent;

public class HPROFAnalysis {

    //HashMap that contains object IDs as a key and an object of type MyWeakReference as the value
    private final ConcurrentHashMap<Reference<? extends Object>, MyWeakReference> weakReferenceMap;

    //HashMap that contains object IDs as a key and a wrapper object of type Counter as the value
    protected static final ConcurrentHashMap<String, Counter> counterMap;

    private static final ReferenceQueue<Object> refqueue;

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

        new ReaperThread().start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                DynamicBypass.activate();

                System.err.println("In shutdown hook!");

                try {
                    PrintWriter pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream("dump.log")));
                    TreeSet<Counter> set = new TreeSet<Counter>(new CounterComparator());

                    for (Iterator<Entry<String, Counter>> iter = counterMap.entrySet().iterator(); iter.hasNext();) {
                        Entry<String, Counter> entry = iter.next();
                        String key = entry.getKey();
                        Counter myCounter = entry.getValue();
                        myCounter.setAllocationSite(key);
                        set.add(myCounter);
                    }

                    int idx = 0;
                    for(Counter counter : set.descendingSet()) {
                        pw.println(idx++ + "\t" + counter.toString());
                    }

                    pw.flush();
                    pw.close();
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

    private class ReaperThread extends Thread {
        public ReaperThread() {
            setDaemon(true);
        }

        public void run() {
            DynamicBypass.activate();

            while(true) {
                try {
                    Object obj = refqueue.remove();

                    MyWeakReference mwr = weakReferenceMap.remove(obj);
                    if(mwr != null) {
                        String fullAllocSiteID = mwr.getFullAllocSiteID();
                        counterMap.get(fullAllocSiteID).decrementCounters(mwr.getSize());
                    } else {
                        System.err.println("[HPROFAnalysis.ReaperThread] Error: the weak reference map does not have an entry for: " + obj.toString());
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
