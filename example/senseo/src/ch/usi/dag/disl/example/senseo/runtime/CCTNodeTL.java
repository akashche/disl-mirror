package ch.usi.dag.disl.example.senseo.runtime;

public final class CCTNodeTL {
    final int methodUID;
    CCTNodeTL parent;
    int calls;

    CCTNodeTL left, right; // siblings in the CCT
    CCTNodeTL callees; // children in the CCT

    final ArgumentNodeTL argsRoot;
    ArgumentNodeTL currentArg;

    final ArgumentNodeTL retValsRoot;

    int allocCount;

    private CCTNodeTL() {
        this.parent = null;
        this.methodUID = -1;

        this.calls = 0;

        currentArg = (argsRoot = null);
        retValsRoot = null;

        allocCount = 0;
    }

    private CCTNodeTL(CCTNodeTL parent, int methodUID, boolean hasOnlyPrimitiveArgs) {
        this.parent = parent;
        this.methodUID = methodUID;

        calls = 1;

        currentArg = (argsRoot = (hasOnlyPrimitiveArgs ? null : ArgumentNodeTL.createRoot()));
        retValsRoot = new ArgumentNodeTL();

        allocCount = 0;
    }

    public static CCTNodeTL createRoot() {
        return new CCTNodeTL();
    }

    CCTNodeTL getOrCreateCallee(int methodUID, boolean hasOnlyPrimitiveArgs) {
        CCTNodeTL n;
        if ((n = callees) == null) {
            return (callees = new CCTNodeTL(this, methodUID, hasOnlyPrimitiveArgs));
        }

        while (true) {
            int n_methodUID;
            if ((n_methodUID = n.methodUID) == methodUID) {
                n.calls++;
                n.resetCurrentArg();
                return n;
            }
            else if (methodUID < n_methodUID) {
                CCTNodeTL lft;
                if ((lft = n.left) == null) {
                    return (n.left = new CCTNodeTL(this, methodUID, hasOnlyPrimitiveArgs));
                }
                n = lft;
            }
            else {
                CCTNodeTL rgt;
                if ((rgt = n.right) == null) {
                    return (n.right = new CCTNodeTL(this, methodUID, hasOnlyPrimitiveArgs));
                }
                n = rgt;
            }
        }
    }

    void profileArgument(Class<?> argClass) {
        currentArg = currentArg.getOrCreateNextArgument(argClass);     
        currentArg.occurrences++;
    }

    void profileReturn(Class<?> retClass) {
        retValsRoot.getOrCreateNextArgument(retClass).occurrences++;
    }

    private void resetCurrentArg() {
        currentArg = argsRoot;
    }

    void profileAlloc() {
        allocCount++;
    }

    void prune(CCTNodeTL callee) {
        left = null;
        right = null;
        callees = callee;

        if(argsRoot != null) {
            argsRoot.nextArgs = null;
        }

        if(retValsRoot != null) {
            retValsRoot.nextArgs = null;
        }

        allocCount = 0;

        calls = 0;
    }
}
