package ch.usi.dag.disl.example.senseo.runtime;

public final class ArgumentNodeTL {
    ArgumentNodeTL prevArg;
    ArgumentNodeTL nextArgs;
    ArgumentNodeTL left, right;

    Class<?> argument;
    int occurrences;

    ArgumentNodeTL() {
        prevArg = null;
        nextArgs = null;
        left = null;
        right = null;

        argument = null;
        occurrences = 0;
    }

    ArgumentNodeTL(ArgumentNodeTL prevArg, Class<?> argument) {
        this.prevArg = prevArg;
        this.argument = argument;
    }

    public static ArgumentNodeTL createRoot() {
        return new ArgumentNodeTL();
    }

    ArgumentNodeTL getOrCreateNextArgument(Class<?> arg) {
        ArgumentNodeTL n;
        if ((n = nextArgs) == null) {
            //System.out.println("[ArgumentNodeTL] 1");
            return (nextArgs = new ArgumentNodeTL(this, arg));
        }

        int hash_jpsp = System.identityHashCode(arg);

        while (true) {
            Class<?> n_arg;
            if ((n_arg = n.argument) == arg) {
                return n;
            }
            else if (hash_jpsp <= System.identityHashCode(n_arg)) {
                ArgumentNodeTL lft;
                if ((lft = n.left) == null) {
                    return (n.left = new ArgumentNodeTL(this, arg));
                }
                n = lft;
            }
            else {
                ArgumentNodeTL rgt;
                if ((rgt = n.right) == null) {
                    return (n.right = new ArgumentNodeTL(this, arg));
                }
                n = rgt;
            }
        }
    }

//    public void dump(StringBuffer buf) { // must be called with activated DIB
//        buf.append('{');
//        dumpArgs(buf, nextArgs, "");
//        buf.append('}');
//    }
//
//    private static void dumpArgs(StringBuffer buf, ArgumentNodeTL n, String path) {
//        if (n != null) {
//            String localPath = path + n.argument.getName();
//
//            if((n.dumpArgs(buf, localPath)) && ((n.left != null) || (n.right != null))) {
//                buf.append(" - ");
//                localPath = "";
//            }
//            dumpArgs(buf, n.left, localPath);
//            dumpArgs(buf, n.right, localPath);
//        }
//    }
//
//    private boolean dumpArgs(StringBuffer buf, String path) {
//        if (nextArgs != null) {
//            String localPath = path + ", ";
//            dumpArgs(buf, nextArgs, localPath);
//            return false;
//        }
//        else {
//            buf.append(path + " = " + occurrences);
//            return true;
//        }
//    }
}
