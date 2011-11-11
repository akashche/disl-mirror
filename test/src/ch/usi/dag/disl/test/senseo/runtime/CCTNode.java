package ch.usi.dag.disl.test.senseo.runtime;


import java.io.*;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import ch.usi.dag.jborat.runtime.DynamicBypass;


public final class CCTNode {

    private static final AtomicIntegerFieldUpdater<CCTNode> callsUpdater;
    private static final AtomicReferenceFieldUpdater<CCTNode, CCTNode> leftUpdater;
    private static final AtomicReferenceFieldUpdater<CCTNode, CCTNode> rightUpdater;
    private static final AtomicReferenceFieldUpdater<CCTNode, CCTNode> calleesUpdater;

    private final AtomicIntegerArray bbs;

    static {
        boolean oldState = DynamicBypass.getAndSet();
        try {
            callsUpdater = AtomicIntegerFieldUpdater.newUpdater(CCTNode.class, "calls");
            leftUpdater = AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "left");
            rightUpdater = AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "right");
            calleesUpdater = AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "callees");
        } finally {
            DynamicBypass.set(oldState);
        }
    }

    @SuppressWarnings("unused")
	private final CCTNode parent;
    private final int methodUID;

    public int index = -1;
    private volatile int calls;
    private volatile CCTNode left, right; // siblings in the CCT
    private volatile CCTNode callees; // children in the CCT

    // Dynamic info
    private final ArgumentNode argsRoot;

    public static CCTNode createRoot() {
        return new CCTNode(null, -1, null, 0, new int[] {});
    }

    private CCTNode(CCTNode parent, int methodUID, ArgumentNodeTL argmnts, int cls, int[] bbs) {
        this.parent = parent;
        this.methodUID = methodUID;

        if(argmnts != null) {
            argsRoot = ArgumentNode.createRoot();
            argsRoot.integrateArguments(argmnts);
        } else {
            argsRoot = null;
        }

        calls = cls;

        this.bbs = new AtomicIntegerArray(bbs);
    }

    private CCTNode getOrCreateCalleeNoProf(int methodUID, ArgumentNodeTL argmnts, int cls, int[] bbs) {
        CCTNode n, allocated;
        if ((n = callees) == null) {
            if (calleesUpdater.compareAndSet(this, null, (allocated = new CCTNode(this, methodUID, argmnts, cls, bbs)))) {
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

                if(argmnts != null) {
                    n.argsRoot.integrateArguments(argmnts);
                }

                n.integrateBBs(bbs);

                return n;
            } 
            else if (methodUID <= n_methodUID) { 
                CCTNode nLeft;
                if ((nLeft = n.left) == null) {
                    if (leftUpdater.compareAndSet(n, null, (allocated = new CCTNode(this, methodUID, argmnts, cls, bbs)))) return allocated;
                    else n = n.left;
                }
                else {
                    n = nLeft;
                }
            } 
            else {
                CCTNode nRight;
                if ((nRight = n.right) == null) {
                    if (rightUpdater.compareAndSet(n, null, (allocated = new CCTNode(this, methodUID, argmnts, cls, bbs)))) return allocated;
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
            CCTNode n = getOrCreateCalleeNoProf(otherCallees.methodUID, otherCallees.argsRoot, otherCallees.calls, otherCallees.bbs);

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

    private void integrateBBs(int[] otherBBs) {
        for(int i = 0; i < otherBBs.length; i++) {
            bbs.addAndGet(i, otherBBs[i]);
        }
    }

    /**
     * Prints a whole tree.
     * Must be invoked on the root node.
     */
    public void dump(PrintStream out) {
        dumpCallees(out, 1, callees);
    }

    private static void dumpCallees(PrintStream out, int level, CCTNode n) {
        if (n != null) {
            StringBuffer buf = new StringBuffer();
            String sig = String.valueOf(n.methodUID);

            buf.append(sig + " " + n.calls + " ");

            buf.append("[ ");
            for(int i = 0; i < n.bbs.length(); i++) {
                buf.append(n.bbs.get(i) + " ");
            }
            buf.append("] ");

            if(n.argsRoot != null) {
                n.argsRoot.dump(buf);
            }

            n.dumpMethod(out, level, buf.toString());
            dumpCallees(out, level, n.left);
            dumpCallees(out, level, n.right);
        }
    }

    private void dumpMethod(PrintStream out, int level, String context) {
        printScope(out, level);
        out.print(context);
        if (callees != null) {
            out.println();
            dumpCallees(out, level + 1, callees);
        }
        else {
            out.println();
        }
    }

    private void printScope(PrintStream out, int level) {
        out.print(level + " ");
    }
}
