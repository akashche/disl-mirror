package ch.usi.dag.disl.example.senseo;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class MethodReturnsObj {
    @GuardMethod
    public static boolean isApplicable(MethodStaticContext msc) {
        String desc = msc.thisMethodDescriptor();
        return !msc.thisMethodName().endsWith("init>")
            && desc.substring(desc.indexOf(')') + 1).startsWith("L");
    }
}
