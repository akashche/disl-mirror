package ch.usi.dag.disl.test.senseo.runtime;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import ch.usi.dag.disl.test.senseo.runtime.arguments.PrimitiveWrapper;
import ch.usi.dag.jborat.runtime.DynamicBypass;


public final class ArgumentNode {
    private static final String SEPARATOR = " ";

    private static final AtomicIntegerFieldUpdater<ArgumentNode> occurrencesUpdater; 
    private static final AtomicReferenceFieldUpdater<ArgumentNode, ArgumentNode> leftUpdater; 
    private static final AtomicReferenceFieldUpdater<ArgumentNode, ArgumentNode> rightUpdater; 
    private static final AtomicReferenceFieldUpdater<ArgumentNode, ArgumentNode> nextArgsUpdater;

    static {
        boolean oldState = DynamicBypass.getAndSet();
        try {
            occurrencesUpdater = AtomicIntegerFieldUpdater.newUpdater(ArgumentNode.class, "occurrences");
            leftUpdater = AtomicReferenceFieldUpdater.newUpdater(ArgumentNode.class, ArgumentNode.class, "left");
            rightUpdater = AtomicReferenceFieldUpdater.newUpdater(ArgumentNode.class, ArgumentNode.class, "right");
            nextArgsUpdater = AtomicReferenceFieldUpdater.newUpdater(ArgumentNode.class, ArgumentNode.class, "nextArgs");
        } finally {
            DynamicBypass.set(oldState);
        }
    }

    private volatile ArgumentNode left, right;
    private volatile ArgumentNode nextArgs;

    private final Class<?> argument;
    private volatile int occurrences;

    private static final Package referencePackage = PrimitiveWrapper.class.getPackage();

    public static ArgumentNode createRoot() {
        return new ArgumentNode(null, null, 0);
    }

    private ArgumentNode(ArgumentNode prevArg, Class<?> argument, int occurrences) {
        this.argument = argument;
        this.occurrences = occurrences;
    }

    public ArgumentNode getOrCreateNextArgument(Class<?> argument, int occurrences) {
        ArgumentNode n, allocated;
        if ((n = nextArgs) == null) {
            if (nextArgsUpdater.compareAndSet(this, null, (allocated = new ArgumentNode(this, argument, occurrences)))) {
                return allocated;
            }
            else {
                n = nextArgs;
            }
        }

        int hash_arg = System.identityHashCode(argument); 

        while (true) {
            Class<?> n_arg;
            if ((n_arg = n.argument) == argument) {
                occurrencesUpdater.addAndGet(n, occurrences); //static updater
                return n;
            } 
            else if (hash_arg <= System.identityHashCode(n_arg)) { 
                ArgumentNode nLeft;
                if ((nLeft = n.left) == null) {
                    if (leftUpdater.compareAndSet(n, null, (allocated = new ArgumentNode(this, argument, occurrences)))) return allocated;
                    else n = n.left;
                }
                else {
                    n = nLeft;
                }
            } 
            else {
                ArgumentNode nRight;
                if ((nRight = n.right) == null) {
                    if (rightUpdater.compareAndSet(n, null, (allocated = new ArgumentNode(this, argument, occurrences)))) return allocated;
                    else n = n.right;
                }
                else {
                    n = nRight;
                }
            } 
        }
    }

    public void integrateArguments(ArgumentNodeTL other) {
        //the root node doesn't contain any stat

        ArgumentNodeTL otherArgs;
        if ((otherArgs = other.nextArgs) != null) {
            integrateNextArguments(otherArgs);
        }
    }

    private void integrateNextArguments(ArgumentNodeTL otherArgs) {
        do {
            ArgumentNode n = getOrCreateNextArgument(otherArgs.argument, otherArgs.occurrences);

            ArgumentNodeTL otherArgsNextArgs;
            if ((otherArgsNextArgs = otherArgs.nextArgs) != null) {
                n.integrateNextArguments(otherArgsNextArgs);
            }

            ArgumentNodeTL otherArgsRight;
            if ((otherArgsRight = otherArgs.right) != null) {
                integrateNextArguments(otherArgsRight);
            }
        } while ((otherArgs = otherArgs.left) != null);
    }

    public void dump(StringBuffer buf) { // must be called with activated DIB
        if(nextArgs != null) {
            dumpArgs(buf, nextArgs, "");
        }
    }

    private static void dumpArgs(StringBuffer buf, ArgumentNode n, String path) {
        if (n != null) {
            String prevPath = path;
            String localPath = prevPath;
            Package pack = n.argument.getPackage();
            if(pack != null && pack.equals(referencePackage)) {
                try {
                    localPath += n.argument.newInstance().toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-10);
                }
            }
            else {
                localPath += n.argument.getName();
            }

            if(n.dumpArgs(buf, localPath)) {
                if(!prevPath.startsWith(SEPARATOR)) {
                    prevPath = SEPARATOR + prevPath;
                }
            }
            dumpArgs(buf, n.left, prevPath);
            dumpArgs(buf, n.right, prevPath);
        }
    }

    private boolean dumpArgs(StringBuffer buf, String path) {
        if (nextArgs != null) {
            String localPath = path + ",";
            dumpArgs(buf, nextArgs, localPath);
            return false;
        }
        else {
            buf.append(path + " " + occurrences);
            return true;
        }
    }

    public ArgumentNode getLeft(){
        return left;
    }

    public ArgumentNode getNextArgs(){
        return nextArgs;
    }

    public ArgumentNode getRight(){
        return right;
    }

    public Class<?> getArgument(){
        return argument;
    }
    public int getOccurrences(){
        return occurrences;
    }
}
