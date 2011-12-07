package ch.usi.dag.disl.example.senseo;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class NotClinit {
    @GuardMethod
    public static boolean isApplicable(MethodStaticContext msc) {
        return (msc.thisMethodName().equals("<clinit>")) ? false : true;
    }
}
