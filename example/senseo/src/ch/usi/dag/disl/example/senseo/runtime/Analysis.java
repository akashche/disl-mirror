package ch.usi.dag.disl.example.senseo.runtime;

import ch.usi.dag.disl.example.senseo.runtime.arguments.Null;
import ch.usi.dag.jborat.runtime.DynamicBypass;


public class Analysis {

    private static final int MAX_TL_TREE_UPDATES = 40000;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MODULE = 8192;

    CCTNodeTL localRoot;
    private CCTNodeTL currentNode;
    private int attempts = 0;
    private int cnt;

    public Analysis() {
        boolean oldState = DynamicBypass.getAndSet();
        try {
            cnt = MAX_TL_TREE_UPDATES + (Thread.currentThread().hashCode() % MODULE);
            currentNode = (localRoot = CCTNodeTL.createRoot());
            CCTManager.getManager().register(Thread.currentThread(), this);
        } finally {
            DynamicBypass.set(oldState);
        }
    }

    public CCTNodeTL onEntry(int methodUID, boolean hasOnlyPrimitiveArgs, int totBBs) {
        currentNode = currentNode.getOrCreateCallee(methodUID, 1, hasOnlyPrimitiveArgs, totBBs);
        return currentNode;
    }

//    public void profileArgument(Class<?> argClass) {//, int pos) {
//        currentNode.profileArgument(argClass);//, pos);
//    }

    public void profileArgument(Object obj) {
        boolean oldState = DynamicBypass.getAndSet();
        try {
            currentNode.profileArgument((obj ==null) ? Null.class : obj.getClass());
        } finally {
            DynamicBypass.set(oldState);
        }
    }

    public void onExit() { //int id) {
//        if(id != currentNode.methodUID) {
//            boolean oldState = DynamicBypass.getAndSet();
//            try {
//                System.err.println("ERROR! id: " + id + " methodUID: " + currentNode.methodUID);
//                System.exit(-1);
//            } finally {
//                DynamicBypass.set(oldState);
//            }
//        }

        currentNode = currentNode.parent;

        if(--cnt == 0) {
            sendCurrentBuffer();
        }
    }

    public void onBB(int index) { //, int id) {
//        if(id != currentNode.methodUID) {
//            boolean oldState = DynamicBypass.getAndSet();
//            try {
//                System.err.println("ERROR! id: " + id + " methodUID: " + currentNode.methodUID);
//                System.exit(-1);
//            } finally {
//                DynamicBypass.set(oldState);
//            }
//        }
        currentNode.profileBB(index);
    }

    public void sendCurrentBuffer() {
        boolean oldState = DynamicBypass.getAndSet();
        try {
            boolean force = (++attempts <= MAX_ATTEMPTS) ? false : true;
        
            if(CCTManager.getManager().merge(localRoot, force)) {
                pruneLocalCCT(currentNode);
                cnt = MAX_TL_TREE_UPDATES + (this.hashCode() % MODULE);
                attempts = 0;
            }
            else {
                cnt = (MAX_TL_TREE_UPDATES / attempts) + (this.hashCode() % MODULE);
            }
        } finally {
            DynamicBypass.set(oldState);
        }
    }

    private static void pruneLocalCCT(CCTNodeTL currentNode) { //extreme pruning that keeps only the elements in the stack
        CCTNodeTL tempNode = currentNode;

        do {
            tempNode.prune();
        } while((tempNode = tempNode.parent) != null);
    }
}
