package ch.usi.dag.disl.test.senseo.runtime;


import java.io.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import ch.usi.dag.jborat.runtime.DynamicBypass;


public final class CCTNode {

	  private static final AtomicIntegerFieldUpdater<CCTNode> callsUpdater; 
	    private static final AtomicReferenceFieldUpdater<CCTNode, CCTNode> leftUpdater;
	    private static final AtomicReferenceFieldUpdater<CCTNode, CCTNode> rightUpdater; 
	    private static final AtomicReferenceFieldUpdater<CCTNode, CCTNode> calleesUpdater; 
	    
	    static {
	    	boolean old = DynamicBypass.get();
	    	DynamicBypass.activate();
	    	try {
	    	  callsUpdater =
	  	        AtomicIntegerFieldUpdater.newUpdater(CCTNode.class, "calls");
	  	      leftUpdater =
	  	        AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "left");
	  	      rightUpdater =
	  	        AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "right");
	  	      calleesUpdater =
	  	        AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "callees");
	    	}finally {
	    		if(old)
	    			DynamicBypass.activate();
	    		else
	    			DynamicBypass.deactivate();
	    	}
	    }

	    private final CCTNode parent;
	    private final int methodUID;

	    public int index = -1;
	    private volatile int calls;
	    private volatile CCTNode left, right; // siblings in the CCT
	    private volatile CCTNode callees; // children in the CCT

	    // Dynamic info
	    private final ArgumentNode args;

	    public static CCTNode createRoot() {
	        return new CCTNode(null, -1, null, 0);
	    }

	    private CCTNode(CCTNode parent, int methodUID, ArgumentNodeTL argmnts, int cls) {
	        this.parent = parent;
	        this.methodUID = methodUID;

	        //TODO: .nextArgs should be removed if argsRoot can be lazily initialized
	        if(argmnts != null && argmnts.nextArgs != null) {
	            args = ArgumentNode.createRoot();
	            args.integrateArguments(argmnts);
	        } else {
	            args = null;
	        }

	        calls = cls;
	    }

//	    public CCTNode getParent() {
//	        return parent;
//	    }
	//
//	    public Object getJPSP() {
//	        return jpsp;
//	    }

	    public int getCalls() {
	        return calls;
	    }

	    public CCTNode getLeft() {
	        return left;
	    }

	    public CCTNode getRight() {
	        return right;
	    }

	    public CCTNode getCallees() {
	        return callees;
	    }

//	    public Set<Class<?>> getArgs() {
//	        return null;
//	    }
	//
//	    public ArgumentNode getArgumentsRoot() {
//	        return args;
//	    }

	    private CCTNode getOrCreateCalleeNoProf(int methodUID, ArgumentNodeTL argmnts, int cls) {
	        CCTNode n, allocated;
	        if ((n = callees) == null) {
	            if (calleesUpdater.compareAndSet(this, null, (allocated = new CCTNode(this, methodUID, argmnts, cls)))) {
	                return allocated;
	            }
	            else {
	                n = callees;
	            }
	        }

	        while (true) {
	            int n_methodUID;
	            if ((n_methodUID = n.methodUID) == methodUID) {
	                callsUpdater.addAndGet(n, cls); //static updaters

//	                try {
	                    //TODO: .nextArgs should be removed if argsRoot can be lazily initialized
	                    if(argmnts.nextArgs != null) {
	                        n.args.integrateArguments(argmnts);
	                    }
//	                } catch(NullPointerException e) {
//	                    e.printStackTrace();
//	                 //   System.out.println(((StaticPart)jpsp).getSignature() + " " + n.args + " " + argmnts);
//	                    System.err.println(jpsp + " ********* " + n.jpsp);
//	                    StringBuffer buf = new StringBuffer();
//	                    argmnts.dump(buf);
//	                    System.err.println(buf.toString());
//	                    System.err.flush();
//	                    System.exit(-1);
//	                }

	                return n;
	            } 
	            else if (methodUID <= n_methodUID) { 
	                CCTNode nLeft;
	                if ((nLeft = n.left) == null) {
	                    if (leftUpdater.compareAndSet(n, null, (allocated = new CCTNode(this, methodUID, argmnts, cls)))) return allocated;
	                    else n = n.left;
	                }
	                else {
	                    n = nLeft;
	                }
	            } 
	            else {
	                CCTNode nRight;
	                if ((nRight = n.right) == null) {
	                    if (rightUpdater.compareAndSet(n, null, (allocated = new CCTNode(this, methodUID, argmnts, cls)))) return allocated;
	                    else n = n.right;
	                }
	                else {
	                    n = nRight;
	                }
	            } 
	        }
	    }

	    public void integrateCCT(CCTNodeTL other) {
	        //the root node doesn't contain any stat

	        CCTNodeTL otherCallees;
	        if ((otherCallees = other.callees) != null) {
	            integrateCallees(otherCallees);
	        }
	    }

	    private void integrateCallees(CCTNodeTL otherCallees) {
	        do {
	            CCTNode n = getOrCreateCalleeNoProf(otherCallees.methodUID, otherCallees.argsRoot, otherCallees.calls);

	            CCTNodeTL otherCalleesCallees;
	            if ((otherCalleesCallees = otherCallees.callees) != null) {
	                n.integrateCallees(otherCalleesCallees);
	            }

	            CCTNodeTL otherCalleesRight;
	            if ((otherCalleesRight = otherCallees.right) != null) {
	                integrateCallees(otherCalleesRight);
	            }
	        } while ((otherCallees = otherCallees.left) != null);
	    }

	    /**
	     * Prints a whole tree.
	     * Shall be invoked on the root node.
	     */
	    public void dump(PrintStream out, boolean isDiSL) { // must be called with activated DIB
	        out.println('{');
	        dumpCallees(out, 1, getCallees(), isDiSL);
	        out.println('}');
	    }

	    private static void dumpCallees(PrintStream out, int level, CCTNode n, boolean isDiSL) {
	        if (n != null) {
	            StringBuffer buf = new StringBuffer();
	            String sig;
	            if(isDiSL) {
	                sig = String.valueOf(n.methodUID);
	            } else {
	                sig = "[CCTNode.dumpCallees] NOT_IMPLEMENTED";//((StaticPart)n.jpsp).getSignature().toString();
	            }

//	            //Verbose format
//	            StringBuffer buf = new StringBuffer(n.jpsp.getSignature().getDeclaringTypeName().replaceAll(" ",""));
//	            buf.append('.');
//	            String sig = n.jpsp.getSignature().toString();//.replaceAll(" ","");

	            buf.append(sig);
	            buf.append(" = ");
	            buf.append(n.getCalls());

	            buf.append(":[");
	            if(n.args != null) {
	                n.args.dump(buf);
	            }
	            buf.append("];");

	            n.dumpMethod(out, level, buf.toString(), isDiSL);
	            dumpCallees(out, level, n.getLeft(), isDiSL);
	            dumpCallees(out, level, n.getRight(), isDiSL);
	        }
	    }

	    private void dumpMethod(PrintStream out, int level, String context, boolean isDiSL) {
	        printScope(out, level);
	        out.print(context);
	        if (getCallees() != null) {
	            out.println(" {");
	            dumpCallees(out, level + 1, getCallees(), isDiSL);
	            printScope(out, level);
	            out.println("}"); 
	        }
	        else {
	            out.println();
	        }
	    }

	    private void printScope(PrintStream out, int level) {
	        while (level-- > 0) {
	            out.print("   ");
	        }
	    }

//	    public void countNodes() {
//	        increaseCount();
//	        countCallees(callees);
//	    }
	//
//	    private static void countCallees(CCTNode n) {
//	        if (n != null) {
//	            increaseCount();
	//
//	            n.count();
//	            countCallees(n.getLeft());
//	            countCallees(n.getRight());
//	        }
//	    }
	//
//	    private void count() {
//	        if (getCallees() != null) {
//	            countCallees(getCallees());
//	        }
//	    }
	//
//	    public static synchronized void increaseCount() {
//	        count++;
//	    }
	//
//	    public static synchronized long getCount() {
//	        return count;
//	    }

}

