package ch.usi.dag.disl.test.senseo.runtime;


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
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

	    private static final CCTManager theManager;
	    private final CCTNode root;

	    private static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

	    private static final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
	    private static final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();

	    private static volatile boolean processCCTFlag = false;

	    private static final int THREAD_LIST_INITIAL_SIZE = 50;
	    private ArrayList<ThreadRef> allThreadRefs = new ArrayList<ThreadRef>(THREAD_LIST_INITIAL_SIZE);

	    static {
	    	boolean old = DynamicBypass.get();
	    	DynamicBypass.activate();
	    	try {
	    		theManager = new CCTManager();
	    	}finally {
	    		if(old) 
	    			DynamicBypass.activate();
	    		else
	    			DynamicBypass.deactivate();
	    	}

	    }

	    private CCTManager() {
	        writeLock.lock();
	        root = CCTNode.createRoot();

	        Runtime.getRuntime().addShutdownHook(new Thread() {
	            public void run() {
	            	System.out.println("DUMPING>>");
	                theManager.processTerminatedThreads();
	                theManager.dump("dump.log", true); // 
	            }
	        });

	        writeLock.unlock();
	    }

	    public static CCTManager getManager() {
	        return theManager;
	    }

	    public void dump(String fileName, boolean isDiSL) {
	        writeLock.lock();
	        try {
	            PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName)));
	            root.dump(out, isDiSL);
	            out.flush();
	            out.close(); //yes, quite a silly mistake...
	        } catch(Exception e) {
	            e.printStackTrace();
	        } finally {
	            writeLock.unlock();
	        }
	    }

	    // public void processCCT(CCTProcessor processor, boolean exclusiveAccess) {
	    //     if(exclusiveAccess) {
	    ////       lock.lock();
	    //     writeLock.lock();
	    //         processCCTFlag = true;
	    //     }
	    //     else {
	    //         readLock.lock();
	    //     }
	    //     try {
	    //         processor.processCCT(root);
	    //     } catch(Exception e) { //TODO: after debugging replace this with a finally { lock.unlock(); }
	    //         e.printStackTrace();
	    //         System.exit(-11);
	    //     }
	    //
	    //     if(exclusiveAccess) {
	    //         processCCTFlag = false;
	    ////       lock.unlock();
	    //     writeLock.unlock();
	    //     }
	    //     else {
	    //         readLock.unlock();
	    //     }
	    // }

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

	    public int processTerminatedThreads() { 
	        int nthreads = allThreadRefs.size();
	        for(int i = 0;i < nthreads;i++) {
	            ThreadRef tr;
	            if((tr = allThreadRefs.get(i)) != null) {
	                if(!tr.thread.isAlive()) {
	                    merge(tr.analysis.localRoot, true);
	                    allThreadRefs.set(i, null);
	                }
	            }
	        }
	        return nthreads;
	    }

//	    public void beforeEnd() {
//	        try {
//	            writeLock.lock();
//	            root = CCTNode.createRoot();
//	        }
//	        finally {
//	            writeLock.unlock();
//	        }
//	    }

}
