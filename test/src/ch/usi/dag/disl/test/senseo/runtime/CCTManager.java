package ch.usi.dag.disl.test.senseo.runtime;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ch.usi.dag.jborat.runtime.DynamicBypass;

public final class CCTManager {
    private class ThreadRef {
        Thread thread;
        Analysis analysis;
        ThreadRef(Thread thread, Analysis analysis) {
            this.thread = thread;
            this.analysis = analysis;
        }
    }

    private static final int THREAD_LIST_INITIAL_SIZE = 50;
    private static final CCTManager theManager;
    private static final ReentrantReadWriteLock rwl;
    private static final ReentrantReadWriteLock.ReadLock readLock;
    private static final ReentrantReadWriteLock.WriteLock writeLock;
    private static volatile boolean processCCTFlag = false;

    private final CCTNode root;
    private ArrayList<ThreadRef> allThreadRefs = new ArrayList<ThreadRef>(THREAD_LIST_INITIAL_SIZE);

    static {
        boolean oldState = DynamicBypass.getAndSet();
        try {
            theManager = new CCTManager();
            rwl = new ReentrantReadWriteLock();
            readLock = rwl.readLock();
            writeLock = rwl.writeLock();
        } finally {
            DynamicBypass.set(oldState);
        }
    }

    private CCTManager() {
//        writeLock.lock();

        root = CCTNode.createRoot();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                boolean oldState = DynamicBypass.getAndSet();
                try {
                    System.out.println("DUMPING>>");
                    theManager.processTerminatedThreads();
                    theManager.dump("dump.log");
                } finally {
                    DynamicBypass.set(oldState);
                }
            }
        });

//        writeLock.unlock();
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

    public boolean merge(CCTNodeTL localRoot, boolean force) {
        if(processCCTFlag && !force) {
            return false;
        }

        readLock.lock();

        try {
            root.integrateCCT(localRoot);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(-12);
        }

        readLock.unlock();

        return true;
    }

    void register(Thread t, Analysis analysis) {
        allThreadRefs.add(new ThreadRef(t, analysis)); //WARNING! No check for duplicated entries!
    }

    public void processTerminatedThreads() {
        for(Iterator<ThreadRef> iter = allThreadRefs.iterator(); iter.hasNext();) {
            ThreadRef tr = iter.next();
            if(!tr.thread.isAlive()) {
                processTerminatedThread(tr, iter);
            } else {
                System.out.println("[CCTManager.processTerminatedThreads] Thread: " + tr.thread.getName() + " is still alive " + tr.thread.getState());
                if(tr.thread.getName().equals("main")) {
                    processTerminatedThread(tr, iter);
                }                
            }
        }
    }

    private void processTerminatedThread(ThreadRef tr, Iterator<ThreadRef> iter) {
        System.out.println("[CCTManager.processTerminatedThread] Processing incomplete buffer of thread: " + tr.thread.getName());
        merge(tr.analysis.localRoot, true);
        iter.remove();
    }
}
