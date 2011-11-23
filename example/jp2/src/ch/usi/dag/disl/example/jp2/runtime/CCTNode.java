package ch.usi.dag.disl.example.jp2.runtime;

/**
 * Copyright (c) 2010 Aibek Sarimbekov, Philippe Moret, Walter Binder, Andreas
 * Sewe
 * 
 * Permission to copy, use, modify (only the included source files, modification
 * of the binaries is not permitted) the software is granted provided this
 * copyright notice appears in all copies.
 * 
 * This software is  provided "as is" without express or implied warranty, and
 * with no claim as to its suitability for any purpose.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import ch.usi.dag.jborat.runtime.DynamicBypass;

//import ch.usi.dag.jborat.runtime.DynamicBypass;
/**
 * A node in the calling context tree (CCT).
 * 
 * @author Aibek Sarimbekov
 * @author Philippe Moret
 * @author Walter Binder
 * @author Andreas Sewe
 */
public final class CCTNode  {
    
    /**
     * The method identifier.
     */
    private final String mid;
    
    /**
     * The execution counts of this method's basic blocks.
     */
    private final AtomicLongArray basicBlockExecutionCounts;
    
    private final CCTNode parent; // Parent in the CCT
    
    private volatile CCTNode left, right; // Siblings in the CCT
    
    private volatile CCTNode callees; // Children in the CCT
    
    private final int bytecodeIndex; // Index of the call-site
    
    private volatile long executionCount; // Number of method executions
    
    private volatile long bytecodesExecuted;
    
    private static CCTNode ROOT;
    
    private static final AtomicReferenceFieldUpdater<CCTNode, CCTNode> leftUpdater, rightUpdater;
    
    private static final AtomicReferenceFieldUpdater<CCTNode, CCTNode> calleesUpdater;
    
    private static final AtomicLongFieldUpdater<CCTNode>callsUpdater;
    
    public static ConcurrentHashMap<String, int[]> bbSizes;
    
    private final int[] myBBSizes;
    
    static {
     	boolean old = DynamicBypass.get();
     	DynamicBypass.activate();
    	try{
            leftUpdater =
                AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "left");
            rightUpdater =
                AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "right");
            calleesUpdater =
                AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "callees");
            callsUpdater = 
                AtomicLongFieldUpdater.newUpdater(CCTNode.class,"executionCount");
            ROOT  = new CCTNode(null, null, 0,-1,0);
            
            bbSizes = new ConcurrentHashMap<String, int[]>();
            	
          
            // SHUTDOWNHOOK
    		Thread shutdownHook = new Thread() {
				@SuppressWarnings("static-access")
				public void run () {
					  
						System.out.println("SHUTDOWN..." + ROOT + " THREAD " + Thread.currentThread());
						Dumper dump = new Dumper();
						dump.out = System.out;
						dump.dump(getRoot());
						
				}	
			};
			Runtime.getRuntime().addShutdownHook(shutdownHook);
            
        }
        finally{
        	if(old)
        		DynamicBypass.activate();
        	else
        		DynamicBypass.deactivate();
        }
    }
    

    public void incrementBytecodeCounter(int count) {
    	bytecodesExecuted += count; 
    }
    public long getExecutedBytecodes() { 
    	return bytecodesExecuted;
    }
    public static CCTNode getRoot() {
        return ROOT;
    }
    
    private CCTNode(CCTNode parent, String MID, int bytecodeIndex, int numerOfBB) {
        this(parent, MID, 1,bytecodeIndex, numerOfBB);
    }
    
    private CCTNode(CCTNode parent, String mid, int executionCount, int bytecodeIndex, int numberOfBB) {
        this.parent = parent;
        this.mid = mid;
        this.executionCount = executionCount;
        this.bytecodeIndex = bytecodeIndex;
        this.basicBlockExecutionCounts = new AtomicLongArray(numberOfBB);
        this.myBBSizes = new int[numberOfBB]; // TODO check if correct
    }
    
    /**
     * @return the parent node in the CCT (or <code>null</code> if this node it
     *   the root)
     */
    public CCTNode getParent() {
        return parent;
    }
    
    /**
     * @return the left sibling in the CCT (if any)
     */
    public CCTNode getLeft() {
        return left;
    }
    
    /**
     * @return the right sibling in the CCT (if any)
     */
    public CCTNode getRight() {
        return right;
    }
    
    public CCTNode getCallees() {
        return callees;
    }
    
    public String getMID() {
        return mid;
    }
    
    public int getBytecodeIndex(){
        return bytecodeIndex;
    }
    
    public long getExecutionCount() {
        return executionCount;
    }
    
    public long getExecutedInstructions() {
        final int[] bbSize = CCTNode.bbSizes.get(mid);
        long result = 0;
        
        for(int i = 0; i < basicBlockExecutionCounts.length(); i++)
            result += basicBlockExecutionCounts.get(i) * bbSize[i];
        
        return result;
    }
    
    public List<Integer> getBasicBlockSizes() {
    
        final int[] basicBlockSizes = CCTNode.bbSizes.get(mid);
        final List<Integer> result = new ArrayList<Integer>(basicBlockSizes.length);
        
        for (int i = 0; i < basicBlockSizes.length; i++)
            result.add(i, basicBlockSizes[i]);
        
        return result;
     
    }
    
    public List<Long> getBasicBlockExecutionCounts() {
        final List<Long> result = new ArrayList<Long>(basicBlockExecutionCounts.length());
        
        for (int i = 0; i < basicBlockExecutionCounts.length(); i++)
            result.add(i, basicBlockExecutionCounts.get(i));
        
        return result;
    }
    
    /**
     * 
     * @return the the array that contains counters for executed basic blocks as String
     */
    public String getBBCountsAsString(){
        return basicBlockExecutionCounts.toString();
    }
    
    // DEBUG
    public void setBBSize(int idx, int size) {
    	if(myBBSizes[idx]==0)
    		myBBSizes[idx] = size;
    }
    
    public String getSizesAsString() {
    	StringBuffer buf = new StringBuffer();
    	for(int i = 0; i<myBBSizes.length; i++) {
    	  buf.append(myBBSizes[i] + " ");
    	}
    	return buf.toString();
    }
    
    /**
     * increments the counter for the executed basic block
     * 
     * @param bbIndex - index of the basic block in the method
     */
    public void profileBB(int bbIndex) {
//        boolean old = DynamicBypass.getAndSet();
//        try {
      //      if (Thread.currentThread().activateBytecodeCounting)
    	if(true) 
                basicBlockExecutionCounts.incrementAndGet(bbIndex);
//        } finally {
//            DynamicBypass.set(old);
//        }
    }
    
    public CCTNode profileCall(String mid, int bytecodeIndex, int numberOfBB) { 
        CCTNode n, allocated;
        int initCalls=0;
        
        if (this == ROOT)
            bytecodeIndex=-1;
        
        if ((n = calleesUpdater.get(this)) == null) {
        	if(true)
    //        if (Thread.currentThread().activateBytecodeCounting)
                initCalls=1;
            if (calleesUpdater.compareAndSet(this, null, (allocated = new CCTNode(this, mid,initCalls,bytecodeIndex, numberOfBB)))) {
                return allocated;
            } else {
                n = calleesUpdater.get(this);
            }
        }
        
        int hash_MID =  System.identityHashCode(mid); 
        
        while (true) {
            String n_MID;
            if ((n_MID = n.mid) == mid && n.bytecodeIndex==bytecodeIndex) {
            	if(true)
            	//if (Thread.currentThread().activateBytecodeCounting)
                    callsUpdater.incrementAndGet(n);
                return n;
            } else if (hash_MID <= System.identityHashCode(n_MID)) { 
                CCTNode nLeft;
                if ((nLeft = leftUpdater.get(n)) == null) {
                	if(true)
                  //  if (Thread.currentThread().activateBytecodeCounting)
                        initCalls=1;
                    if (leftUpdater.compareAndSet(n, null, (allocated = new CCTNode(this, mid, initCalls, bytecodeIndex, numberOfBB)))) {
                        return allocated;
                    } else {
                        n = leftUpdater.get(n);
                    }
                } else {
                    n = nLeft;
                }
            } else {
                CCTNode nRight;
                if ((nRight = rightUpdater.get(n)) == null) {
                	//
              
                  //  if (Thread.currentThread().activateBytecodeCounting)
                	if(true)
                        initCalls=1;
                    if (rightUpdater.compareAndSet(n, null, (allocated = new CCTNode(this, mid, initCalls, bytecodeIndex, numberOfBB)))) {
                        return allocated;
                    } else {
                        n = rightUpdater.get(n);
                    }
                } else {
                    n = nRight;
                }
            } 
        }
    }
    
   
   /* 
    public List<? extends CallingContextTreeNode> collectCallees() {
        List<CCTNode> callees = new ArrayList<CCTNode>();
        accumulateSiblings(this.getCallees(), callees);
        Collections.sort(callees, new Comparator<CCTNode>() {
            
            public int compare(CCTNode lhs, CCTNode rhs) {
                
                return lhs.getBytecodeIndex() - rhs.getBytecodeIndex();
            }
        });
        return callees;
    }
    */
    
    @SuppressWarnings("unused")
	private static void accumulateSiblings(CCTNode n, List<CCTNode> accumulator) {
        if (n == null) return;
        
        accumulator.add(n);
        accumulateSiblings(n.getLeft(), accumulator);
        accumulateSiblings(n.getRight(), accumulator);
    }
}
