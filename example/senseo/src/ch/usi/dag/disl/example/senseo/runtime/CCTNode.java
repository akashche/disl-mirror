package ch.usi.dag.disl.example.senseo.runtime;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public final class CCTNode {

    private static final AtomicReferenceFieldUpdater<CCTNode, CCTNode> leftUpdater;
    private static final AtomicReferenceFieldUpdater<CCTNode, CCTNode> rightUpdater;
    private static final AtomicReferenceFieldUpdater<CCTNode, CCTNode> calleesUpdater;

    static {
//        DynamicBypass.activate();
        leftUpdater = AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "left");
        rightUpdater = AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "right");
        calleesUpdater = AtomicReferenceFieldUpdater.newUpdater(CCTNode.class, CCTNode.class, "callees");
//        DynamicBypass.deactivate();
    }

    private final CCTNode parent;
    private final int methodUID;

    private volatile CCTNode left, right; // siblings in the CCT
    private volatile CCTNode callees; // children in the CCT

    private final AtomicInteger calls;
    private final AtomicInteger allocCount;

    private final ArgumentNode argsRoot;
    private final ArgumentNode retValsRoot;

    public static CCTNode createRoot() {
        return new CCTNode(null, -1, null, 0, null, 0);
    }

    private CCTNode(CCTNode parent, int methodUID, ArgumentNodeTL argmnts, int allocCount, ArgumentNodeTL retVals, int cls) {
        this.parent = parent;
        this.methodUID = methodUID;

        if(argmnts != null) {
            argsRoot = ArgumentNode.createRoot();
            argsRoot.integrateArguments(argmnts);
        } else {
            argsRoot = null;
        }

        calls = new AtomicInteger(cls);

        this.allocCount = new AtomicInteger(allocCount);

        if(retVals != null) {
            retValsRoot = ArgumentNode.createRoot();
            retValsRoot.integrateArguments(retVals);
        } else {
            retValsRoot = null;
        }
    }

    private CCTNode getOrCreateCalleeNoProf(int methodUID, ArgumentNodeTL argmnts, int allocCount, ArgumentNodeTL retVals, int cls) {
        CCTNode n, allocated;
        if ((n = callees) == null) {
            if (calleesUpdater.compareAndSet(this, null, (allocated = new CCTNode(this, methodUID, argmnts, allocCount, retVals, cls)))) {
                return allocated;
            }
            else {
                n = callees;
            }
        }

        while(true) {
            int n_methodUID;
            if((n_methodUID = n.methodUID) == methodUID) {
                n.calls.addAndGet(cls);

                if(argmnts != null) {
                    n.argsRoot.integrateArguments(argmnts);
                }

                n.allocCount.addAndGet(allocCount);

                if(retVals.nextArgs != null) {
                    n.retValsRoot.integrateArguments(retVals);
                }

                return n;
            } 
            else if(methodUID < n_methodUID) { 
                CCTNode nLeft;
                if ((nLeft = n.left) == null) {
                    if (leftUpdater.compareAndSet(n, null, (allocated = new CCTNode(this, methodUID, argmnts, allocCount, retVals, cls)))) return allocated;
                    else n = n.left;
                }
                else {
                    n = nLeft;
                }
            } 
            else {
                CCTNode nRight;
                if ((nRight = n.right) == null) {
                    if (rightUpdater.compareAndSet(n, null, (allocated = new CCTNode(this, methodUID, argmnts, allocCount, retVals, cls)))) return allocated;
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
            CCTNode n = getOrCreateCalleeNoProf(otherCallees.methodUID, otherCallees.argsRoot, otherCallees.allocCount, otherCallees.retValsRoot, otherCallees.calls);

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

    public void prune() {
        left = null;
        right = null;
        callees = null;

        calls.set(0);
        allocCount.set(0);

        if(argsRoot != null) argsRoot.prune();
        if(retValsRoot != null) retValsRoot.prune();
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

            buf.append(sig + " " + n.calls.get() + " ");

            if(n.argsRoot != null) {
                n.argsRoot.dump(buf);
            }

            buf.append(" { " + n.allocCount.get() + " } ");

            if(n.retValsRoot != null) {
                if(n.retValsRoot.getNextArgs() != null) {
                    buf.append(" ~ ");
                    n.retValsRoot.dump(buf);
                }
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
