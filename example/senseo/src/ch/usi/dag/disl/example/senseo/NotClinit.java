package ch.usi.dag.disl.example.senseo;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodSC;

public class NotClinit {
    @GuardMethod
    public static boolean isApplicable(MethodSC msc) {
        return (msc.thisMethodName().equals("<clinit>")) ? false : true;
    }
}
