package ch.usi.dag.disl.example.senseo.runtime;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ch.usi.dag.disl.dynamicbypass.DynamicBypass;

public final class CCTManager {
    private static final CCTManager theManager;
    private static final ReentrantReadWriteLock rwl;
    private static final ReentrantReadWriteLock.ReadLock readLock;
    private static final ReentrantReadWriteLock.WriteLock writeLock;
    private final ReentrantLock rl;

    private final CCTNode root;
    private final IdentityHashMap<Thread, Analysis> allThreadRefs;

    private volatile boolean doNotMerge = false;

    static {
//        DynamicBypass.activate();
        theManager = new CCTManager();
        rwl = new ReentrantReadWriteLock();
        readLock = rwl.readLock();
        writeLock = rwl.writeLock();
//        DynamicBypass.deactivate();
    }

    private CCTManager() {
        root = CCTNode.createRoot();
        allThreadRefs = new IdentityHashMap<Thread, Analysis>();
        rl = new ReentrantLock();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                DynamicBypass.activate();
                System.out.println("DUMPING>>");
                theManager.processTerminatedThreads();
                theManager.dump("dump.log");
                DynamicBypass.deactivate();
            }
        });
    }

    public static CCTManager getManager() {
        return theManager;
    }

    public void dump(String fileName) {
        writeLock.lock();
        try {
            PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName)));
            root.dump(out);
            out.flush();
            out.close();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
    }

    public void merge(CCTNodeTL localRoot) {
        if(!doNotMerge) {
            if(localRoot != null) {
                if(localRoot.callees != null) {
                    readLock.lock();
                    root.integrateCCT(localRoot);
                    readLock.unlock();                
                }
            }
        }
    }

    public static void pruneAndRunGC(int gcRuns) {
        DynamicBypass.activate();
        writeLock.lock();
        try {
            CCTManager cctManager = CCTManager.getManager();
            cctManager.doNotMerge = true;
            System.out.println("[CCTManager] Pruning and running GC...");
            cctManager.root.prune();

            for(int i = 0; i < gcRuns; i++) {
                System.runFinalization();
                System.gc();
                try { Thread.sleep(100); } catch(InterruptedException e) { e.printStackTrace(); }
            }

            cctManager.doNotMerge = false;
            System.out.println("[CCTManager] ...done!");
        } finally {
            writeLock.unlock();
            DynamicBypass.deactivate();
        }
    }

    void register(Thread t, Analysis analysis) {
        rl.lock();
        Analysis prev = allThreadRefs.put(t, analysis);
        if(prev != null) {
            System.err.println("[CCTManager] Thread " + t + " already registered! NEW: " + analysis + " OLD: " + prev);
            System.exit(-5);
        }
        rl.unlock();
    }

    private void processTerminatedThreads() {
        rl.lock();
        for(Iterator<Entry<Thread, Analysis>> iter = allThreadRefs.entrySet().iterator(); iter.hasNext();) {
            Entry<Thread, Analysis> entry = iter.next();
            Thread t = entry.getKey();
            Analysis a = entry.getValue();
            if(!t.isAlive()) {
                processTerminatedThread(t, a, iter);
            }
            else {
//                System.out.println("[CCTManager.processTerminatedThreads] Thread: " + t.getName() + " is still alive " + t.getState());
                if(t.getName().equals("main")) {
                    processTerminatedThread(t, a, iter);
                }                
            }
        }
        rl.unlock();
    }

    private void processTerminatedThread(Thread t, Analysis a, Iterator<Entry<Thread, Analysis>> iter) {
//        System.out.println("[CCTManager.processTerminatedThread] Processing incomplete buffer of thread: " + t.getName());
        merge(a.localRoot);
        iter.remove();
    }

    public static void beforeBenchmarkEnd() {
        getManager().processTerminatedThreads();
    }
}
