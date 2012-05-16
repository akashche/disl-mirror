package ch.usi.dag.disl.example.senseo;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class NotClinit {
    @GuardMethod
    public static boolean isApplicable(MethodStaticContext msc) {
        if(
            msc.thisMethodName().equals("<clinit>")
            || msc.thisMethodName().equals("<init>")
            && (
                msc.thisClassName().equals("java/lang/Object")
                || msc.thisClassName().equals("java/lang/Thread")
            )
        ) {
            return false;
        }
        return true;
    }
}
