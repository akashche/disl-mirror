package ch.usi.dag.disl.test.senseo.runtime;

public final class CCTNodeTL {
    final int methodUID;
    CCTNodeTL parent;
    int calls;

    CCTNodeTL left, right; // siblings in the CCT
    CCTNodeTL callees; // children in the CCT

    // Dynamic info
    final ArgumentNodeTL argsRoot = ArgumentNodeTL.createRoot();
    ArgumentNodeTL currentArg;

    CCTNodeTL() {
        this.parent = null;
        this.methodUID = -1;
        //        argsRoot = null;

        this.calls = 0;
    }

    private CCTNodeTL(CCTNodeTL parent, int methodUID, int calls) {
        this.parent = parent;
        this.methodUID = methodUID;

        this.calls = calls;
    }

    public static CCTNodeTL createRoot() {
        return new CCTNodeTL();
    }

    CCTNodeTL getOrCreateCallee(int methodUID, int calls) {
        CCTNodeTL n;
        if ((n = callees) == null) {
            return (callees = new CCTNodeTL(this, methodUID, calls));
        }

        while (true) {
            int n_methodUID;
            if ((n_methodUID = n.methodUID) == methodUID) {
                n.calls += calls;
                return n;
            }
            else if (methodUID <= n_methodUID) {
                CCTNodeTL lft;
                if ((lft = n.left) == null) {
                    return (n.left = new CCTNodeTL(this, methodUID, calls));
                }
                n = lft;
            }
            else {
                CCTNodeTL rgt;
                if ((rgt = n.right) == null) {
                    return (n.right = new CCTNodeTL(this, methodUID, calls));
                }
                n = rgt;
            }
        }
    }

//    void profileArguments(JoinPoint jp) {
//        Object[] argsType;
//        if((argsType = jp.getArgs()) != null) {
//            if(argsRoot == null) {
//                argsRoot = ArgumentNodeTL.createRoot();
//            }
//
//            currentArg = argsRoot;
//            for(Object argType : argsType) {
//                if(argType != null) {
//                    currentArg = currentArg.getOrCreateNextArgument(argType.getClass());
//                }
//                else {
//                    currentArg = currentArg.getOrCreateNextArgument(Null.class);
//                }
//            }
//            currentArg.occurrences++;
//        }
//    }

    void profileArgument(Class<?> argClass, int pos) {
//        if(pos == 0) {
//            if(argsRoot == null) {
//                argsRoot = ArgumentNodeTL.createRoot();
//            }
//
//            currentArg = argsRoot;
//        }
        currentArg = currentArg.getOrCreateNextArgument(argClass);     
        currentArg.occurrences++;
    }

    void resetCurrentArg() {
        currentArg = argsRoot;
    }
}
