package ch.usi.dag.disl.example.senseo;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

// check if none of the arguments is an Object 
// --> if "L" is not in the description of the arguments
public class ConstructorHasOnlyPrimitiveArgs {
    @GuardMethod
    public static boolean isApplicable(MethodStaticContext msc) {
        String desc = msc.thisMethodDescriptor();
        return
            msc.thisMethodName().equals("<init>")
            && !msc.thisClassName().equals("java/lang/Object")
            && !msc.thisClassName().equals("java/lang/Thread")
            && !desc.substring(0, desc.indexOf(')')).contains("L");
    }
}
