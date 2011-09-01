package ch.usi.dag.disl.test.senseo.runtime;


public class Analysis {
	
	 private static final int MAX_TL_TREE_UPDATES = 40000;
	    private static final int MAX_ATTEMPTS = 5;
//	    private static final int MODULE = 8192;

	    CCTNodeTL localRoot;
	    private CCTNodeTL currentNode;
	    private int attempts = 0;
	    private int cnt = MAX_TL_TREE_UPDATES;// + (Thread.currentThread().hashCode() % MODULE);

	    public Analysis() {
	        currentNode = (localRoot = CCTNodeTL.createRoot());
	        CCTManager.getManager().register(Thread.currentThread(), this);
	    }


	    public CCTNodeTL onMethodEntry(int methodUID) {
	    	currentNode = currentNode.getOrCreateCallee(methodUID, 1);
	    	currentNode.resetCurrentArg();
	    	return currentNode;
	    }

	    public void profileArgument(Class<?> argClass, int pos) {
	        currentNode.profileArgument(argClass, pos);
	    }


	    public void onMethodExit() {
	        currentNode = currentNode.parent;

	        if(--cnt == 0) {
	            boolean force = (++attempts <= MAX_ATTEMPTS) ? false : true;

	            if(CCTManager.getManager().merge(localRoot, force)) {
	                pruneLocalCCT(currentNode);
	                cnt = MAX_TL_TREE_UPDATES;// + (this.hashCode() % MODULE);
	                attempts = 0;
	            }
	            else {
	                cnt = MAX_TL_TREE_UPDATES;//(MAX_TL_TREE_UPDATES / attempts) + (this.hashCode() % MODULE);
	            }
	        }
	    }

	    private static void pruneLocalCCT(CCTNodeTL currentNode) { //extreme pruning that keeps only the elements in the stack
	        CCTNodeTL tempNode;
	        CCTNodeTL currNode = currentNode;

	        currNode.left = null;
	        currNode.right = null;
	        currNode.callees = null;

	        currNode.argsRoot.nextArgs = null;
//	        currNode.argsRoot = null;
//	        if(currNode.argsRoot != null) {
//	            currNode.argsRoot = ArgumentNodeTL.createRoot();
//	        }

	        currNode.calls = 0;

	        while((tempNode = currNode.parent) != null) {
	            tempNode.callees = currNode;
	            tempNode.left = null;
	            tempNode.right = null;

                tempNode.argsRoot.nextArgs = null;

//	            tempNode.argsRoot = null;
//	            if(currNode.argsRoot != null) {
//	                currNode.argsRoot = ArgumentNodeTL.createRoot();
//	            }

	            tempNode.calls = 0;
	            currNode = tempNode;
	        }
	    }


}