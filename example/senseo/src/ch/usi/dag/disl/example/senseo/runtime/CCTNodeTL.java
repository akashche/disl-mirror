package ch.usi.dag.disl.example.senseo.runtime;

public final class CCTNodeTL {
    final int methodUID;
    CCTNodeTL parent;
    int calls;

    CCTNodeTL left, right; // siblings in the CCT
    CCTNodeTL callees; // children in the CCT

    // Dynamic info
    final ArgumentNodeTL argsRoot;// = ArgumentNodeTL.createRoot();
    ArgumentNodeTL currentArg;

    final int[] bbs;

    private CCTNodeTL() {
        this.parent = null;
        this.methodUID = -1;

        this.calls = 0;

        currentArg = (argsRoot = null);

        bbs = null;
    }

    private CCTNodeTL(CCTNodeTL parent, int methodUID, int calls, boolean hasOnlyPrimitiveArgs, int totBBs) {
        this.parent = parent;
        this.methodUID = methodUID;

        this.calls = calls;

        currentArg = (argsRoot = (hasOnlyPrimitiveArgs ? null : ArgumentNodeTL.createRoot()));

        bbs = new int[totBBs];
    }

    public static CCTNodeTL createRoot() {
        return new CCTNodeTL();
    }

    CCTNodeTL getOrCreateCallee(int methodUID, int calls, boolean hasOnlyPrimitiveArgs, int totBBs) {
        CCTNodeTL n;
        if ((n = callees) == null) {
            return (callees = new CCTNodeTL(this, methodUID, calls, hasOnlyPrimitiveArgs, totBBs));
        }

        while (true) {
            int n_methodUID;
            if ((n_methodUID = n.methodUID) == methodUID) {
                n.calls += calls;
                n.resetCurrentArg();
                return n;
            }
            else if (methodUID <= n_methodUID) {
                CCTNodeTL lft;
                if ((lft = n.left) == null) {
                    return (n.left = new CCTNodeTL(this, methodUID, calls, hasOnlyPrimitiveArgs, totBBs));
                }
                n = lft;
            }
            else {
                CCTNodeTL rgt;
                if ((rgt = n.right) == null) {
                    return (n.right = new CCTNodeTL(this, methodUID, calls, hasOnlyPrimitiveArgs, totBBs));
                }
                n = rgt;
            }
        }
    }

    void profileArgument(Class<?> argClass) {
        currentArg = currentArg.getOrCreateNextArgument(argClass);     
        currentArg.occurrences++;
    }

    private void resetCurrentArg() {
        currentArg = argsRoot;
    }

    void profileBB(int index) {
        bbs[index]++;
    }

    void prune() {
        left = null;
        right = null;
        callees = null;

        if(argsRoot != null) {
            argsRoot.nextArgs = null;
        }

        for(int i = 0; i < bbs.length; i++) {
            bbs[i] = 0;
        }

        calls = 0;
    }
}
