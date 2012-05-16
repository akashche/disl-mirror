package ch.usi.dag.disl.example.senseo.runtime;

import ch.usi.dag.disl.dynamicbypass.DynamicBypass;
import ch.usi.dag.disl.example.senseo.runtime.arguments.Null;

public class Analysis {

    private static final int MAX_TL_TREE_UPDATES = 40000;
    private static final int MODULE = 8192;

    CCTNodeTL localRoot;
    private CCTNodeTL currentNode;
    private int cnt;

    public Analysis() {
        DynamicBypass.activate();
        cnt = MAX_TL_TREE_UPDATES + (Thread.currentThread().hashCode() % MODULE);
        currentNode = (localRoot = CCTNodeTL.createRoot());
        CCTManager.getManager().register(Thread.currentThread(), this);
        DynamicBypass.deactivate();
    }

    public CCTNodeTL onEntry(int methodUID, boolean hasOnlyPrimitiveArgs) {
        currentNode = currentNode.getOrCreateCallee(methodUID, hasOnlyPrimitiveArgs);
        return currentNode;
    }

    public void onExit() {
        currentNode = currentNode.parent;

        if(--cnt == 0) {
            DynamicBypass.activate();
            sendCurrentBuffer();
            DynamicBypass.deactivate();
        }
    }

    public void onExit(Object obj) {
        DynamicBypass.activate();
        currentNode.profileReturn((obj == null) ? Null.class : obj.getClass());
        currentNode = currentNode.parent;

        if(--cnt == 0) {
            sendCurrentBuffer();
        }

        DynamicBypass.deactivate();
    }

    public void profileArgument(Object obj) {
        DynamicBypass.activate();
        currentNode.profileArgument((obj == null) ? Null.class : obj.getClass());
        DynamicBypass.deactivate();
    }

    public void onAlloc() {
        currentNode.profileAlloc();
    }

    //THE BYPASS MUST BE ACTIVE
    private void sendCurrentBuffer() {
        CCTManager.getManager().merge(localRoot);
        pruneLocalCCT(currentNode);
        cnt = MAX_TL_TREE_UPDATES + (this.hashCode() % MODULE);
    }

    //extreme pruning that keeps only the elements in the stack
    //THE BYPASS MUST BE ACTIVE
    private static void pruneLocalCCT(CCTNodeTL currentNode) {
        CCTNodeTL tempNode1 = currentNode;
        CCTNodeTL tempNode2 = null;

        do {
            tempNode1.prune(tempNode2);
            tempNode2 = tempNode1;
        } while((tempNode1 = tempNode1.parent) != null);
    }
}
